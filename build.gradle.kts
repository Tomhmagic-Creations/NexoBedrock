import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    id("java")
    alias(idofrontLibs.plugins.mia.kotlin.jvm)
    alias(idofrontLibs.plugins.mia.papermc)
    alias(idofrontLibs.plugins.mia.copyjar)
    alias(idofrontLibs.plugins.mia.autoversion)
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.8"
}

val pluginVersion: String by project
version = pluginVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.nexomc.com/snapshots")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT") //NMS
    compileOnly("com.nexomc:nexo:0.7.0")
}

copyJar {
    destPath.set(project.findProperty("nexo_ia_plugin_path").toString())
    jarName.set(jarName.orNull ?: "${project.name}-${pluginVersion}-${System.currentTimeMillis()}.jar")
    if ("dev" in jarName.get() && destPath.orNull != null) File(destPath.get()).listFiles { file -> file.extension == "jar" }?.forEach {
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
}
