package io.github.liangforstudy.minemobs.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.liangforstudy.minemobs.client.gui.MineMobsSettingsScreen;

/** Mod Menu's "Configure" button. Only loaded when Mod Menu is installed. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MineMobsSettingsScreen::new;
    }
}
