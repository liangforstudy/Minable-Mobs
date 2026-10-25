package io.github.liangforstudy.minemobs.client.gui;

import io.github.liangforstudy.minemobs.client.ClientMiningState;
import io.github.liangforstudy.minemobs.client.MineMobsClientConfig;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayMapping;
import io.github.liangforstudy.minemobs.client.overlay.BreakOverlayMappings;
import io.github.liangforstudy.minemobs.config.MineMobsConfig;
import io.github.liangforstudy.minemobs.network.UpdateSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Minable Mobs settings. General: mining on/off (server) and the tap-or-hold delay (yours).
 * Mobs: each mob's break time and netherite time with "as hard as" profiles, and its break overlay
 * mode. Server settings are sent when the screen closes, and only ops (or the singleplayer owner)
 * can change them.
 */
public class MineMobsSettingsScreen extends Screen {
    private enum Tab { GENERAL, MOBS }

    private static final int MUTED = 0xA0A0A0;
    private static final int HIGHLIGHT = 0xFFE08A;
    private static Tab lastTab = Tab.GENERAL;

    @Nullable private final Screen parent;
    /** Our copy of the server settings, edited here and sent on close. Null when not in a world with the mod. */
    @Nullable private final MineMobsConfig working;
    private final boolean canEdit;
    private final List<BreakProfile> profiles = BreakProfile.all();
    private final List<TimeField> timeFields = new ArrayList<>();
    private boolean dirty;
    private Tab tab = lastTab;

    /** Selected row in the Mobs tab; null = the defaults row. */
    @Nullable private EntityType<?> selected;
    private String search = "";
    private double listScroll;
    @Nullable private MobList list;

    public MineMobsSettingsScreen(@Nullable Screen parent) {
        super(Component.literal("Minable Mobs"));
        this.parent = parent;
        MineMobsConfig server = ClientMiningState.serverSettings();
        this.working = server == null ? null : MineMobsConfig.fromJson(server.toJson());
        this.canEdit = server != null && ClientMiningState.canEditServerSettings();
    }

    @Override
    protected void init() {
        timeFields.clear();
        list = null;
        int tabWidth = 100;
        Button general = addRenderableWidget(Button.builder(Component.literal("General"), b -> switchTab(Tab.GENERAL))
                .bounds(width / 2 - tabWidth - 2, 26, tabWidth, 20).build());
        Button mobs = addRenderableWidget(Button.builder(Component.literal("Mobs"), b -> switchTab(Tab.MOBS))
                .bounds(width / 2 + 2, 26, tabWidth, 20).build());
        general.active = tab != Tab.GENERAL;
        mobs.active = tab != Tab.MOBS;

        if (tab == Tab.GENERAL) initGeneral();
        else initMobs();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    // ---------------------------------------------------------------- General

    private void initGeneral() {
        int left = width / 2 - 155;
        int controls = left + 160;
        int y = 60;

        label(left, y, 150, Component.literal("Mob mining"));
        if (working != null) {
            CycleButton<Boolean> enabled = addRenderableWidget(CycleButton.onOffBuilder(working.enabled).displayOnlyValue()
                    .create(controls, y, 150, 20, Component.literal("Mob mining"), (button, value) -> {
                        working.enabled = value;
                        dirty = true;
                    }));
            enabled.active = canEdit;
            enabled.setTooltip(Tooltip.create(Component.literal("Turns mob mining on or off for everyone on this server (same as /minemobs).")));
        } else {
            label(controls, y, 150, Component.literal("Join a world first").withColor(MUTED));
        }

        y += 26;
        label(left, y, 150, Component.literal("Tap or hold delay"));
        timeField(controls, y, 60, MineMobsClientConfig.get().holdDelayTicks, 0, MineMobsClientConfig.MAX_HOLD_DELAY, true, ticks -> {
            MineMobsClientConfig.get().holdDelayTicks = ticks;
            MineMobsClientConfig.save();
        });
        unitSwitcher(controls + 64, y, 86);
        y += 24;
        addRenderableWidget(new MultiLineTextWidget(left, y,
                Component.literal("With a pickaxe, press a mob shorter than this to hit it, hold longer to mine it. "
                        + "The wait counts toward mining. 0 = mine right away (no tap to hit)."), font)
                .setMaxWidth(310).setColor(MUTED));

        if (working != null && !canEdit) {
            addRenderableWidget(new MultiLineTextWidget(left, height - 54,
                    Component.literal("Server settings are read-only: only operators can change them."), font)
                    .setMaxWidth(310).setColor(HIGHLIGHT));
        }
    }

    // ---------------------------------------------------------------- Mobs

    private void initMobs() {
        int listX = 8;
        int listWidth = Math.min(180, (int) (width * 0.38));
        int panelX = listX + listWidth + 12;
        int panelWidth = Math.min(300, width - panelX - 8);

        EditBox searchBox = new EditBox(font, listX, 54, listWidth, 18, Component.literal("Search mobs"));
        searchBox.setHint(Component.literal("Search mobs…").withColor(MUTED));
        searchBox.setValue(search);
        searchBox.setResponder(text -> {
            search = text;
            if (list != null) list.fill();
        });
        addRenderableWidget(searchBox);

        list = new MobList(minecraft, listX, 76, listWidth, height - 76 - 34);
        list.fill();
        list.setScrollAmount(listScroll);
        addRenderableWidget(list);

        int y = 54;
        Component name = selected == null ? Component.literal("All mobs (default)") : selected.getDescription();
        label(panelX, y, panelWidth, name.copy().withColor(HIGHLIGHT));
        y += 12;
        label(panelX, y, panelWidth, Component.literal(selected == null
                ? "Used by every mob without its own times"
                : EntityType.getKey(selected) + (hasOwnTimes() ? "" : " · using the defaults")).withColor(MUTED));
        y += 18;

        if (working == null) {
            addRenderableWidget(new MultiLineTextWidget(panelX, y,
                    Component.literal("Break times come from the server. Join a world that has Minable Mobs to see and change them."), font)
                    .setMaxWidth(panelWidth).setColor(MUTED));
            y += 36;
        } else {
            y = timeRow(panelX, y, panelWidth, "Break time", "Any pickaxe except netherite.",
                    breakTicks(), this::setBreakTicks);
            y = timeRow(panelX, y, panelWidth, "Netherite time", "Netherite pickaxe (#minemobs:fast_mining_tools).",
                    netheriteTicks(), this::setNetheriteTicks);

            if (selected != null) {
                Button reset = addRenderableWidget(Button.builder(Component.literal("Use the defaults"), b -> {
                    working.mobs.remove(EntityType.getKey(selected).toString());
                    dirty = true;
                    rebuild();
                }).bounds(panelX, y, 120, 20).build());
                reset.active = canEdit && hasOwnTimes();
                y += 26;
            }
            if (!canEdit) {
                label(panelX, y, panelWidth, Component.literal("Read-only: only operators can change these.").withColor(HIGHLIGHT));
                y += 16;
            }
        }

        if (selected != null) {
            EntityType<?> type = selected;
            label(panelX, y + 6, 90, Component.literal("Break overlay"));
            CycleButton<BreakOverlayMapping.Mode> mode = addRenderableWidget(CycleButton
                    .<BreakOverlayMapping.Mode>builder(m -> Component.literal(m == BreakOverlayMapping.Mode.FIT ? "Fit" : "Projection"))
                    .withValues(BreakOverlayMapping.Mode.values())
                    .withInitialValue(BreakOverlayMappings.fromFiles(type).mode())
                    .displayOnlyValue()
                    .create(panelX + 94, y, 100, 20, Component.literal("Break overlay"),
                            (button, value) -> BreakOverlayMappings.setMode(type, value)));
            mode.setTooltip(Tooltip.create(Component.literal(
                    "Fit: the cracks follow each face. Projection: one crack texture per block across the mob. "
                            + "Your choice is saved in config/minemobs/break_overlay/ (face tweaks are kept).")));
        }
    }

    /** "Label  [ value ] ‹ unit ›" plus a row of profile buttons. Returns the next free y. */
    private int timeRow(int x, int y, int width, String name, String tooltip, int ticks, IntConsumer setter) {
        label(x, y + 6, 90, Component.literal(name)).setTooltip(Tooltip.create(Component.literal(tooltip)));
        TimeField field = timeField(x + 94, y, 56, ticks, 0, MineMobsConfig.MAX_TICKS, canEdit, setter);
        unitSwitcher(x + 154, y, 70);
        y += 22;
        int buttonWidth = (width - 3 * 2) / profiles.size();
        for (int i = 0; i < profiles.size(); i++) {
            BreakProfile profile = profiles.get(i);
            Button button = addRenderableWidget(Button.builder(Component.literal(profile.shortName()), b -> field.setTicks(profile.ticks()))
                    .bounds(x + i * (buttonWidth + 2), y, buttonWidth, 20).build());
            button.setTooltip(Tooltip.create(Component.literal(profile.longName() + ": " + TimeText.both(profile.ticks()))));
            button.active = canEdit;
        }
        return y + 28;
    }

    private int breakTicks() {
        return selected == null ? working.defaultBreakTicks : working.breakTicks(selected);
    }

    private int netheriteTicks() {
        return selected == null ? working.defaultNetheriteTicks : working.netheriteTicks(selected);
    }

    private void setBreakTicks(int ticks) {
        if (selected == null) working.defaultBreakTicks = ticks;
        else ownTimes().breakTicks = ticks;
        dirty = true;
    }

    private void setNetheriteTicks(int ticks) {
        if (selected == null) working.defaultNetheriteTicks = ticks;
        else ownTimes().netheriteTicks = ticks;
        dirty = true;
    }

    private MineMobsConfig.MobTimes ownTimes() {
        return working.mobs.computeIfAbsent(EntityType.getKey(selected).toString(), id -> new MineMobsConfig.MobTimes());
    }

    private boolean hasOwnTimes() {
        if (working == null || selected == null) return false;
        MineMobsConfig.MobTimes times = working.mobs.get(EntityType.getKey(selected).toString());
        return times != null && !times.isEmpty();
    }

    private boolean hasOwnTimes(EntityType<?> type) {
        if (working == null) return false;
        MineMobsConfig.MobTimes times = working.mobs.get(EntityType.getKey(type).toString());
        return times != null && !times.isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private StringWidget label(int x, int y, int width, Component text) {
        return addRenderableWidget(new StringWidget(x, y, width, 9, text, font).alignLeft());
    }

    private TimeField timeField(int x, int y, int width, int ticks, int min, int max, boolean editable, IntConsumer onChange) {
        TimeField field = addRenderableWidget(new TimeField(font, x, y, width, 20, Component.literal("Time"), ticks, min, max, onChange));
        field.setEditable(editable);
        field.setTooltip(Tooltip.create(Component.literal("Type a value, or hover and scroll to change it one tick at a time.")));
        timeFields.add(field);
        return field;
    }

    private void unitSwitcher(int x, int y, int width) {
        addRenderableWidget(new UnitSwitcher(x, y, width, 20, () -> timeFields.forEach(TimeField::refresh)));
    }

    private void switchTab(Tab next) {
        tab = next;
        lastTab = next;
        rebuild();
    }

    private void select(@Nullable EntityType<?> type) {
        selected = type;
        rebuild();
    }

    private void rebuild() {
        if (list != null) listScroll = list.getScrollAmount();
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (dirty && canEdit && working != null && ClientPlayNetworking.canSend(UpdateSettingsPayload.TYPE)) {
            ClientPlayNetworking.send(new UpdateSettingsPayload(working.toJson()));
        }
        minecraft.setScreen(parent);
    }

    // ---------------------------------------------------------------- mob list

    private final class MobList extends ObjectSelectionList<MobEntry> {
        MobList(Minecraft minecraft, int x, int y, int width, int height) {
            super(minecraft, width, height, y, 20);
            setX(x);
        }

        /** (Re)fill from the registry: every living entity type, filtered by the search box. */
        void fill() {
            String query = search.trim().toLowerCase(Locale.ROOT);
            List<MobEntry> entries = new ArrayList<>();
            if (query.isEmpty()) entries.add(new MobEntry(null));
            BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(DefaultAttributes::hasSupplier)
                    .map(MobEntry::new)
                    .filter(entry -> query.isEmpty() || entry.matches(query))
                    .sorted(Comparator.comparing(entry -> entry.name.getString()))
                    .forEach(entries::add);
            replaceEntries(entries);
            for (MobEntry entry : entries) {
                if (entry.type == selected) setSelected(entry);
            }
        }

        @Override
        public int getRowWidth() {
            return width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return getX() + width - 6;
        }
    }

    private final class MobEntry extends ObjectSelectionList.Entry<MobEntry> {
        @Nullable final EntityType<?> type;
        final Component name;
        final ItemStack icon;

        MobEntry(@Nullable EntityType<?> type) {
            this.type = type;
            this.name = type == null ? Component.literal("All mobs (default)") : type.getDescription();
            SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
            this.icon = egg == null ? ItemStack.EMPTY : new ItemStack(egg);
        }

        boolean matches(String query) {
            return name.getString().toLowerCase(Locale.ROOT).contains(query)
                    || EntityType.getKey(type).toString().contains(query);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (!icon.isEmpty()) graphics.renderItem(icon, left + 1, top + 1);
            boolean custom = type != null && hasOwnTimes(type);
            String text = font.plainSubstrByWidth(name.getString(), width - 26);
            graphics.drawString(font, text, left + 20, top + 5, custom ? HIGHLIGHT : 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            select(type);
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", name);
        }
    }
}
