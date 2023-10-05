plugins {
    id("io.cloudflight.autoconfigure-settings") version "1.0.1"
}

rootProject.name = "license-gradle-plugin"

configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setScopeCalc(calcScopeFromCommitMessages())
}
