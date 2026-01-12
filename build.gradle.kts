plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

val pluginVersion = "1.1.1"

group = "person.cyl"
version = pluginVersion

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    intellijPlatform {
//        intellijIdeaCommunity("2025.2.4")
        local(file("D:\\Android\\Studio"))
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        plugin("Dart", "500.0.0")
    }

}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "221.1"
        }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    patchPluginXml {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
