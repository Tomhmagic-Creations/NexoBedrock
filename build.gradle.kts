import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    id("java")
    alias(idofrontLibs.plugins.mia.kotlin.jvm)
    alias(idofrontLibs.plugins.mia.papermc)
    alias(idofrontLibs.plugins.mia.copyjar)
    alias(idofrontLibs.plugins.mia.autoversion)
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18" apply false
}

val pluginVersion: String by project
version = pluginVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.nexomc.com/snapshots")
    mavenLocal()
}

dependencies {
    //paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT") //NMS
    compileOnly("com.nexomc:nexo:1.8.0")

    implementation(project(path = ":core"))
    implementation(project(path = ":v1_20_R3"))
    implementation(project(path = ":v1_20_R4"))
    implementation(project(path = ":v1_21_R1"))
    implementation(project(path = ":v1_21_R2"))
    implementation(project(path = ":v1_21_R3"))
    implementation(project(path = ":v1_21_R6"))
}

tasks {
    shadowJar {
        relocate("kotlin.", "com.nexomc.libs.kotlin.")
        relocate("com.jeff_media", "com.nexomc.libs")
    }
}

copyJar {
    destPath.set(project.findProperty("nexo_plugin_path").toString())
    jarName.set(jarName.orNull ?: "${project.name}-${pluginVersion}-${System.currentTimeMillis()}.jar")
    if (destPath.orNull != null) File(destPath.get()).listFiles { file -> file.extension == "jar" }?.forEach {
        if (jarName.get().startsWith(it.name.substringBefore("-"))) it.delete()
    }
}

bukkitPluginYaml {
    main = "com.portalgg.nexobedrock.NexoBedrock"
    name = "NexoBedrock"
    apiVersion = "1.20"
    this.version = pluginVersion
    authors.add("boy0000")
    load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
    depend = listOf("Nexo")
    softDepend = listOf("Geyser-Spigot", "floodgate")
}
