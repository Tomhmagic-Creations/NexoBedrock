plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.autoversion")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

repositories {
    gradlePluginPortal()
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    google()
    mavenLocal()
}


dependencies {
    compileOnly("com.nexomc:nexo:1.8.0")

    // Shaded
    implementation(project(":core"))
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT") //NMS
}

tasks {

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}