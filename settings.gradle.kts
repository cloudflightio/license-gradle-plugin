plugins {
    id("io.cloudflight.autoconfigure-settings") version "0.9.0"
}

rootProject.name = "license-gradle-plugin"

configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setScopeCalc(calcScopeFromCommitMessages())
}