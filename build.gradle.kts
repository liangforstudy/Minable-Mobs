// Built once per Minecraft version by Stonecutter (versions/<mc>/ reuse this script).
plugins {
    // Applies the right Loom variant for the Minecraft version
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.parchmentmc.org") { name = "ParchmentMC" } }
        filter { includeGroup("org.parchmentmc.data") }
    }
    // MCP Fabric: dev client only, lets Claude see and drive the running game over MCP (.mcp.json)
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // Mod Menu — optional: the config screen only shows up when it's installed
    exclusiveContent {
        forRepository { maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" } }
        filter { includeGroup("com.terraformersmc") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    if (sc.current.parsed >= "26.1") { // unobfuscated: no Parchment needed
        loomx.applyMojangMappings()
    } else {
        // Mojang names + Parchment parameter names and javadoc
        "mappings"(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${property("deps.parchment")}@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}") { isTransitive = false }
    modLocalRuntime("com.terraformersmc:modmenu:${property("deps.modmenu")}") { isTransitive = false }
    modLocalRuntime("maven.modrinth:mcpfabric:${property("deps.mcpfabric")}") // never bundled
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("fabric.mod.json") { expand(props) }
        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies them to build/libs/{mod version}/"
        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
