package io.cloudflight.license.gradle

import io.cloudflight.jsonwrapper.tracker.Report
import io.cloudflight.license.gradle.test.util.ProjectFixture
import io.cloudflight.license.gradle.test.util.normalizedOutput
import io.cloudflight.license.gradle.test.util.useFixture
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.exists

class LicensePluginTest {

    @Test
    fun `up-to-date handling works correctly for clfLicenseReport`(): Unit =
        licenseFixture("single-java-module") {
            val result = run("clean", "clfLicenseReport")
            assertThat(result.task(":clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val result2 = run("clfLicenseReport")
            assertThat(result2.task(":clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }

    @Test
    fun `clfLicenseReport is called with clean build`(): Unit =
        licenseFixture("single-java-module") {
            val result = runCleanBuild()
            assertThat(result.task(":clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

    @Test
    fun `license JSON report is created `(): Unit =
        licenseFixture("single-java-module") {
            run("clean", "clfLicenseReport", "clfCreateTrackerReport")
            val licenseReport = fixtureDir.resolve("build/licenses/license-report.json")
            assertThat(licenseReport).exists()
            val licenseRecords = LicenceRecordReader.readFromFile(licenseReport.toFile())
            assertThat(licenseRecords).isNotEmpty
        }

    @Test
    @Disabled
    fun `clfLicenseReport is not created when the module is not a java module`(): Unit =
        licenseFixture("single-empty-module") {
            val result = runBuild()
            assertThat(result.task(":clfLicenseReport")).isNull()
        }

    @Test
    fun `multi-module build`(): Unit =
        licenseFixture("multi-module") {
            val result = runCleanBuild()
            assertThat(result.task(":sample-ui:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-api:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-server:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            assertThat(result.normalizedOutput).doesNotContain("Execution optimizations have been disabled for task")

            println(result.normalizedOutput)

            val dependenciesOfUi =
                Report.readFromFile(this.fixtureDir.resolve("sample-ui/build/tracker/dependencies.json").toFile())

            assertThat(dependenciesOfUi.compile).isNotEmpty


            val result2 = runBuild()
            assertThat(result2.normalizedOutput).doesNotContain("Execution optimizations have been disabled for task")

        }

    @Test
    fun `multi-module build npm`(): Unit =
        licenseFixture("multi-module") {
            val nodeModules = fixtureDir.resolve("sample-ui/node_modules")
            if (nodeModules.exists()) {
                assertThat(nodeModules.toFile().deleteRecursively()).isTrue
            }

            val result = run("clean", ":sample-server:clfLicenseReport")
            println(result.normalizedOutput)
            assertThat(result.task(":sample-server:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-ui:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-ui:npmInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-api:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val licenseReport = fixtureDir.resolve("sample-server/build/licenses/license-report.json")
            assertThat(licenseReport).exists()
            val licenseRecords = LicenceRecordReader.readFromFile(licenseReport.toFile())
            val licenseEntry = licenseRecords.find { it.project == "methods" }
            assertThat(licenseEntry).isNotNull
            assertThat(licenseEntry?.licenses).isNotEmpty

            val result2 = run(":sample-server:clfLicenseReport")
            assertThat(result2.task(":sample-ui:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(result2.task(":sample-ui:npmInstall")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(result2.task(":sample-api:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(result2.task(":sample-server:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }

    @Test
    fun `multi-module build npm run trackerReport`(): Unit =
        licenseFixture("multi-module") {
            val nodeModules = fixtureDir.resolve("sample-ui/node_modules")
            if (nodeModules.exists()) {
                assertThat(nodeModules.toFile().deleteRecursively()).isTrue
            }

            val result = run("clean", "clfCreateTrackerReport")
            println(result.normalizedOutput)
            assertThat(result.normalizedOutput).doesNotContain("license can't be parsed")
            assertThat(result.task(":sample-server:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-ui:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-ui:clfCreateTrackerReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-ui:npmInstall")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":sample-api:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val dependencies =
                Report.readFromFile(fixtureDir.resolve("sample-ui/build/tracker/dependencies.json").toFile())
            assertThat(dependencies.development.find { it.artifact.contains("@colors:colors") }).isNotNull
            assertThat(dependencies.development.find { it.artifact.contains("@npm:fsevents") }).isNull()
        }

    @Test
    fun `test-suite dependencies are collected correctly`(): Unit =
        licenseFixture("single-suite-module") {
            val result = run("clean", "clfCreateTrackerReport")
            println(result.normalizedOutput)
            assertThat(result.normalizedOutput).doesNotContain("license can't be parsed")
            assertThat(result.task(":clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(result.task(":clfCreateTrackerReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

            val dependencies =
                Report.readFromFile(fixtureDir.resolve("build/tracker/dependencies.json").toFile())
            assertThat(dependencies.test.find { it.artifact.contains("org.assertj:assertj-core") }).isNotNull
            assertThat(dependencies.test.find { it.artifact.contains("io.cloudflight.json:json-wrapper") }).isNotNull
        }

    @Test
    fun `android-module build`():Unit = licenseFixture("android-module") {
        val result = runCleanBuild()
        assertThat(result.task(":app:clfLicenseReport")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        assertThat(result.normalizedOutput).doesNotContain("Execution optimizations have been disabled for task")

        val dependenciesOfApp =
            Report.readFromFile(this.fixtureDir.resolve("app/build/tracker/dependencies.json").toFile())

        assertThat(dependenciesOfApp.compile).isNotEmpty
        assertThat(dependenciesOfApp.compile.find { it.artifact.contains("org.assertj:assertj-core") }).isNotNull

        val result2 = runBuild()
        assertThat(result2.normalizedOutput).doesNotContain("Execution optimizations have been disabled for task")
    }
}


private fun <T : Any> licenseFixture(
    fixtureName: String,
    gradleVersion: String? = null,
    testWork: ProjectFixture.() -> T
): T =
    useFixture(Paths.get(""), fixtureName, gradleVersion, testWork)
