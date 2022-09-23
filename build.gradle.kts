plugins {
    id("io.cloudflight.autoconfigure-gradle") version "0.6.5"
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.18.0"
    alias(libs.plugins.kotlin.serialization)
    id("java-library")
}

description = "License Gradle Plugin"
group = "io.cloudflight.license.gradle"

if (System.getenv("RELEASE") != "true") {
    version = "$version-SNAPSHOT"
}

autoConfigure {
    java {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendorName.set("Cloudflight")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.maven.artifact)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.spdx.catalog)
    implementation(libs.json.wrapper)
    implementation(libs.httpclient)
    implementation(libs.gradle.node.plugin)

    testImplementation(libs.bundles.testImplementationDependencies)

    testRuntimeOnly(libs.junit.engine)
}

tasks.compileTestKotlin.configure {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlin.io.path.ExperimentalPathApi"
    }
}

tasks.test {
    inputs.dir(layout.projectDirectory.dir("./src/test/fixtures"))
}

java {
    withJavadocJar()
}

pluginBundle {
    website = "https://github.com/cloudflightio/license-gradle-plugin"
    vcsUrl = "https://github.com/cloudflightio/license-gradle-plugin.git"
    tags = listOf("license", "dependencies", "report")
}

gradlePlugin {
    plugins {
        create("license-gradle-plugin") {
            id = "io.cloudflight.license-gradle-plugin"
            displayName = "License Gradle Plugin"
            description = "Collects and prints all dependencies along with their licenses including SPDX support"
            implementationClass = "io.cloudflight.license.gradle.LicensePlugin"
        }
    }
}

tasks.withType<Jar>() {
    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("NOTICE"))
}