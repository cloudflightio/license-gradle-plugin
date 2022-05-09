plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
    id("maven-publish")
    alias(libs.plugins.nexus.publishing)
    alias(libs.plugins.kotlin.serialization)
    id("signing")
}

description = "License Gradle Plugin"
group = "io.cloudflight.license.gradle"

if (System.getenv("RELEASE") != "true") {
    version = "$version-SNAPSHOT"
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/cloudflightio/license-gradle-plugin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                inceptionYear.set("2022")
                organization {
                    name.set("Cloudflight GmbH")
                    url.set("https://cloudflight.io")
                }
                developers {
                    developer {
                        id.set("cloudflight")
                        name.set("Cloudflight Team")
                        email.set("opensource@cloudflight.io")
                    }
                }
                scm {
                    connection.set("scm:ggit@github.com:cloudflightio/license-gradle-plugin.git")
                    developerConnection.set("scm:git@github.com:cloudflightio/license-gradle-plugin.git")
                    url.set("https://github.com/cloudflightio/license-gradle-plugin")
                }
            }
        }
    }
}

signing {
    setRequired {
        System.getenv("PGP_SECRET") != null
    }
    useInMemoryPgpKeys(System.getenv("PGP_SECRET"), System.getenv("PGP_PASSPHRASE"))
    sign(publishing.publications.getByName("maven"))
}
