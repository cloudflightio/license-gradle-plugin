package io.cloudflight.license.gradle

import com.github.gradle.node.npm.task.NpmInstallTask
import io.cloudflight.license.gradle.task.LicenseReportTask
import io.cloudflight.license.gradle.tracker.task.CreateTrackerReportTask
import io.cloudflight.license.gradle.tracker.task.SendToTrackerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.language.jvm.tasks.ProcessResources

class LicensePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        // TODO replace this static reference to something much better
        Licenses.dependencyFactory = (target as ProjectInternal).services.get(DependencyFactory::class.java)

        val licenseDir = target.layout.buildDirectory.dir("licenses")
        val reportTask = target.tasks.create("clfLicenseReport", LicenseReportTask::class.java) {
            it.group = "cloudflight"
            it.description = "Outputs license report"
            it.htmlFile.set(licenseDir.get().file("report/META-INF/NOTICE.html"))
            it.jsonFile.set(licenseDir.get().file("license-report.json"))
            it.setLicenseOverwrites(LicenseDefinitionReader().loadLicenseOverrides(target))
        }

        val createReportTask = target.tasks.create("clfCreateTrackerReport", CreateTrackerReportTask::class.java) {
            it.group = "cloudflight"
            it.licenseFile.set(reportTask.jsonFile)
            it.outputFile.set(target.layout.buildDirectory.file("tracker/dependencies.json"))
        }

        val trackerUrl = System.getenv("CLOUDFLIGHT_TRACKER_URL")
        if (trackerUrl?.isNotEmpty() == true) {
            target.tasks.create("clfReportToTracker", SendToTrackerTask::class.java) {
                it.group = "cloudflight"
                it.trackerFile.set(createReportTask.outputFile)
                it.trackerUrl.set(trackerUrl)
            }
        }

        if (target.tasks.names.contains(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)) {
            target.tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources::class.java).get()
                .from(reportTask.htmlFile) {
                    it.into("META-INF")
                }
        } else {
            // Even though the processReleaseResources Task on Android exists, it does not have a Copy functionality
            //   like the Java equivalent 'processResources'
            // A Copy class can only be created by a Gradle-API and not directly.
            target.tasks.create("copyTask",Copy::class.java).from(reportTask.htmlFile) {
                it.into("META-INF")
            }
            target.tasks.named("copyTask").get().enabled = false
        }

        target.afterEvaluate {proj ->
            val npmInstallTask = proj.tasks.findByName(NpmInstallTask.NAME)
            if (npmInstallTask != null && reportTask.getPackageLockJson().isPresent) {
                reportTask.dependsOn(npmInstallTask)
            }

            GradleUtils.findRuntimeProjectDependencies(proj).forEach { dp ->
                LicenseBuildUtils.withTask(dp, reportTask.name) {
                    val reportTaskOfDependency = dp.tasks.findByName(reportTask.name)
                    if (reportTaskOfDependency != null) {
                        reportTask.dependsOn(reportTaskOfDependency)
                    }
                }
            }

            val configuration = LicenseBuildUtils.createDocumentationConfiguration(proj)
            val licenses = proj.artifacts.add(configuration.name, reportTask.jsonFile) {
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