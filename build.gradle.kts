plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.18.0"
    alias(libs.plugins.kotlin.serialization)
}

description = "License Gradle Plugin"
group = "io.cloudflight.license.gradle"

if (System.getenv("RELEASE") != "true") {
    version = "$version-SNAPSHOT"
}

if (System.getenv("GRADLE_PUBLISH_KEY") != null) {
    project.setProperty("gradle.publish.key", System.getenv("GRADLE_PUBLISH_KEY"))
    project.setProperty("gradle.publish.secret", System.getenv("GRADLE_PUBLISH_SECRET"))
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
    useJUnitPlatform()
    inputs.dir(layout.projectDirectory.dir("./src/test/fixtures"))
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
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
    manifest {
        val compiler  = javaToolchains.compilerFor(java.toolchain).get().metadata
        val createdBy = compiler.javaRuntimeVersion + " (" + compiler.vendor + ")"
        val vendorName: String by project.extra

        attributes(
            "Created-By" to createdBy,
            "Implementation-Vendor" to vendorName,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }

    from(layout.projectDirectory.file("LICENSE"))
    from(layout.projectDirectory.file("NOTICE"))
}