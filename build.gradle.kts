plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "io.github.tuanha1305"
version = "1.0.1"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Build against IntelliJ Community 2024.3 (broad compatibility floor).
        // Final compatibility range is patched into plugin.xml below.
        intellijIdeaCommunity("2024.3")

        // Required by IntelliJ Platform Gradle Plugin v2 for the
        // instrumentCode task. Skipping this fails with "No Java Compiler
        // dependency found" even if we don't use form/DSL instrumentation.
        instrumentationTools()

        // Test framework for the IntelliJ Platform.
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 233 = 2023.3, give us back-compat to ~2 years of releases.
            sinceBuild = "233"
            // No upper bound: keep working on future IDE versions until proven otherwise.
            untilBuild = provider { null }
        }
    }
    publishing {
        // Set the JetBrains Marketplace token via env var TCVN3_MARKETPLACE_TOKEN
        // before running ./gradlew publishPlugin.
        token = providers.environmentVariable("TCVN3_MARKETPLACE_TOKEN")
    }
    signing {
        // Optional plugin signing. Skipped when env vars are not present.
        certificateChain = providers.environmentVariable("TCVN3_CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("TCVN3_PRIVATE_KEY")
        password = providers.environmentVariable("TCVN3_PRIVATE_KEY_PASSWORD")
    }
}

tasks {
    test {
        useJUnit()
    }
    // Produce a regular `.zip` artifact at build/distributions/.
    buildPlugin {
        archiveBaseName.set("tcvn3-jetbrains-plugin")
    }
}
