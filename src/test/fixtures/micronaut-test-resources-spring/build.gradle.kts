plugins {
    id("java")
    id("io.cloudflight.license-gradle-plugin")
    id("io.micronaut.test-resources") version "3.7.10"
    id("org.springframework.boot") version "3.5.7"
}

version = "0.0.1"
group = "io.cloudflight"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(platform("io.cloudflight.platform.spring:platform-spring-bom:2.4.10"))
    testImplementation(platform("io.cloudflight.platform.spring:platform-spring-test-bom:2.4.10"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    //runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("io.cloudflight.platform.spring:platform-spring-test")
    testImplementation("io.cloudflight.platform.spring:platform-spring-test-archunit")
    testImplementation("io.cloudflight.platform.spring:platform-spring-test-bdd")
    testImplementation("io.cloudflight.platform.spring:platform-spring-test-jpa")
    testImplementation("io.cloudflight.platform.spring:platform-spring-test-testcontainers")

    testResourcesImplementation("io.cloudflight.testresources.springboot:springboot-testresources-jdbc-mariadb:0.6.0")
}