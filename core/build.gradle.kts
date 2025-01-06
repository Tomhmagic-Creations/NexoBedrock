plugins {
    id("maven-publish")
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.papermc")
    id("com.mineinabyss.conventions.autoversion")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.11"
    alias(idofrontLibs.plugins.kotlinx.serialization)
}

repositories {
    gradlePluginPortal()
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
    maven("https://repo.opencollab.dev/main/")
    google()
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT") //NMS
    compileOnly("com.nexomc:nexo:0.9-dev")
    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
}