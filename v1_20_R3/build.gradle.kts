plugins {
    id("com.mineinabyss.conventions.kotlin.jvm")
    id("com.mineinabyss.conventions.autoversion")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.11"
}

repositories {
    gradlePluginPortal()
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
    google()
    mavenLocal()
}


dependencies {
    compileOnly("com.nexomc:nexo:0.9-dev")

    // Shaded
    implementation(project(":core"))
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT") //NMS
}

tasks {

    build {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}