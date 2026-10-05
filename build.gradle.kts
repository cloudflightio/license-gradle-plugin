plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "2.0.0"
    alias(libs.plugins.kotlin.serialization)
}

description = "License Gradle Plugin"
group = "io.cloudflight.license.gradle"

autoConfigure {
    java {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
        vendorName = "Cloudflight"
    }
    kotlin {
        kotlinVersion = libs.versions.kotlin.get()
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.maven.artifact)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.spdx.catalog)
    implementation(libs.json.wrapper)
    implementation(libs.httpclient)
    implementation(libs.snakeyaml)
    implementation(libs.gradle.node.plugin)

    testImplementation(libs.bundles.testImplementationDependencies)

    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.compileKotlin.configure {
    compilerOptions {
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

gradlePlugin {
    website = "https://github.com/cloudflightio/license-gradle-plugin"
    vcsUrl = "https://github.com/cloudflightio/license-gradle-plugin.git"
    plugins {
        create("license-gradle-plugin") {
            id = "io.cloudflight.license-gradle-plugin"
            displayName = "License Gradle Plugin"
            description = "Collects and prints all dependencies along with their licenses including SPDX support"
            implementationClass = "io.cloudflight.license.gradle.LicensePlugin"
            tags = listOf("license", "dependencies", "report")
        }
    }
}

tasks.withType<Jar> {
    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("NOTICE"))
}
