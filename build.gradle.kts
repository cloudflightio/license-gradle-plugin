plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.1.0"
    alias(libs.plugins.kotlin.serialization)
}

description = "License Gradle Plugin"
group = "io.cloudflight.license.gradle"

autoConfigure {
    java {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendorName.set("Cloudflight")
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
}

tasks.compileKotlin.configure {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }
}

gradlePlugin {
    website.set("https://github.com/cloudflightio/license-gradle-plugin")
    vcsUrl.set("https://github.com/cloudflightio/license-gradle-plugin.git")
    plugins {
        create("license-gradle-plugin") {
            id = "io.cloudflight.license-gradle-plugin"
            displayName = "License Gradle Plugin"
            description = "Collects and prints all dependencies along with their licenses including SPDX support"
            implementationClass = "io.cloudflight.license.gradle.LicensePlugin"
            tags.set(listOf("license", "dependencies", "report"))
        }
    }
}

tasks.withType<Jar>() {
    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("NOTICE"))
}

tasks.withType<Test> {
    // we wanna set the java Launcher to 17 here in order to be able set higher java compatibility
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}