plugins {
    id("dev.kikugie.stonecutter")
    // Loom for obfuscated versions (<= 1.21.x); loom-back-compat applies it per version.
    // 1.17.x is the newest line that runs on a Java 21 Gradle JVM (1.18+ needs Java 25).
    id("net.fabricmc.fabric-loom-remap") version "1.17.21" apply false
}

stonecutter active "1.21.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}
