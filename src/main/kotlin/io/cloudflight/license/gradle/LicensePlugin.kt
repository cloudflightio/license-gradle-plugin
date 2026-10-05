package io.cloudflight.license.gradle

import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.yarn.task.YarnInstallTask
import io.cloudflight.license.gradle.task.LicenseReportTask
import io.cloudflight.license.gradle.tracker.task.CreateTrackerReportTask
import io.cloudflight.license.gradle.tracker.task.SendToTrackerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.language.jvm.tasks.ProcessResources

class LicensePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val licenseDir = target.layout.buildDirectory.dir("licenses")

        val reportTask = target.tasks.register("clfLicenseReport", LicenseReportTask::class.java) {
            it.group = "cloudflight"
            it.description = "Outputs license report"
            it.htmlFile.set(licenseDir.map { dir -> dir.file("report/META-INF/NOTICE.html") })
            it.jsonFile.set(licenseDir.map { dir -> dir.file("license-report.json") })
            it.setLicenseOverwrites(LicenseDefinitionReader().loadLicenseOverrides(target))
        }

        val createReportTask = target.tasks.register("clfCreateTrackerReport", CreateTrackerReportTask::class.java) {
            it.group = "cloudflight"
            it.licenseFile.set(reportTask.flatMap { t -> t.jsonFile })
            it.outputFile.set(target.layout.buildDirectory.file("tracker/dependencies.json"))
        }

        val trackerUrl = System.getenv("CLOUDFLIGHT_TRACKER_URL")
        if (trackerUrl?.isNotEmpty() == true) {
            target.tasks.register("clfReportToTracker", SendToTrackerTask::class.java) {
                it.group = "cloudflight"
                it.trackerFile.set(createReportTask.flatMap { t -> t.outputFile })
                it.trackerUrl.set(trackerUrl)
            }
        }

        target.plugins.withId("java") {
            target.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources::class.java).configure {
                it.from(reportTask.flatMap { t -> t.htmlFile }) { spec ->
                    spec.into("META-INF")
                }
            }
        }

        // Even though the processReleaseResources Task on Android exists, it does not have a Copy functionality
        //   like the Java equivalent 'processResources'. Also, its destDir is not set.
        // A Copy class can only be created by a Gradle-API and not directly.
        target.plugins.withId("com.android.application") {
            val copyTask = target.tasks.register("copyTask", Copy::class.java) { copy ->
                copy.destinationDir = target.layout.buildDirectory.dir("resources").get().asFile
                copy.from(reportTask.flatMap { t -> t.htmlFile }) { spec ->
                    spec.into("META-INF")
                }
            }

            // this task is only available after evaluation
            target.tasks.matching { it.name == "processReleaseResources" }.configureEach {
                it.finalizedBy(copyTask)
            }
        }

        target.afterEvaluate { proj ->
            val reportTaskInstance = reportTask.get()

            val npmInstallTask = proj.tasks.findByName(NpmInstallTask.NAME)
            if (npmInstallTask != null && reportTaskInstance.getPackageLockJson().isPresent) {
                reportTask.configure { it.dependsOn(npmInstallTask) }
            }

            val yarnInstallTask = proj.tasks.findByName(YarnInstallTask.NAME)
            if (yarnInstallTask != null && reportTaskInstance.getYarnLock().isPresent) {
                reportTask.configure { it.dependsOn(yarnInstallTask) }
            }

            GradleUtils.findRuntimeProjectDependencies(proj).forEach { dp ->
                LicenseBuildUtils.withTask(dp, reportTask.name) {
                    val reportTaskOfDependency = dp.tasks.findByName(reportTask.name)
                    if (reportTaskOfDependency != null) {
                        reportTask.configure { it.dependsOn(reportTaskOfDependency) }
                    }
                }
            }

            val configuration = LicenseBuildUtils.createDocumentationConfiguration(proj)
            val licenses = proj.artifacts.add(configuration.name, reportTask.flatMap { it.jsonFile }) {
                it.type = LicenseReportTask.REPORT_TYPE
                it.classifier = LicenseReportTask.REPORT_CLASSIFIER
                it.builtBy(reportTask)
            }

            configuration.outgoing {
                it.artifact(licenses)
            }
        }
    }
}