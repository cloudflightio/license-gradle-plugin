package io.cloudflight.license.gradle.tracker.task

import com.github.gradle.node.NodeExtension
import io.cloudflight.jsonwrapper.cleancode.CleanCodeReport
import io.cloudflight.jsonwrapper.license.LicenseRecord
import io.cloudflight.jsonwrapper.tracker.Artifact
import io.cloudflight.jsonwrapper.tracker.BuildTool
import io.cloudflight.jsonwrapper.tracker.Project
import io.cloudflight.jsonwrapper.tracker.Report
import io.cloudflight.license.gradle.GradleUtils
import io.cloudflight.license.gradle.npm.NpmLicenseParser
import io.cloudflight.license.gradle.tracker.model.npm.NpmPackageParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Category
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CreateTrackerReportTask : DefaultTask() {

    private val json = Json { prettyPrint = true }

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFile
    abstract val licenseFile: RegularFileProperty

    @InputFile
    @Optional
    fun getPackageLockJson(): Provider<RegularFile> {
        val node = project.extensions.findByType(NodeExtension::class.java)
        return node?.nodeProjectDir?.file(NpmLicenseParser.PACKAGE_LOCK_JSON) ?: project.provider { null }
    }

    @InputFile
    @Optional
    fun getPackageJson(): Provider<RegularFile> {
        val node = project.extensions.findByType(NodeExtension::class.java)
        return node?.nodeProjectDir?.file(NpmLicenseParser.PACKAGE_JSON) ?: project.provider { null }
    }

    @Suppress("ComplexCondition")
    @TaskAction
    fun createTrackerReport() {
        val report = Report()

        report.buildTool = BuildTool.Gradle
        report.buildToolVersion = project.gradle.gradleVersion
        report.pluginVersion = CreateTrackerReportTask::class.java.`package`.implementationVersion
        report.project = toTrackerProject(project)

        val compileArtifacts = mutableListOf<Artifact>()

        compileArtifacts.addAll(
            collectDependencies(
                project.configurations,
                GradleUtils.getCompileClasspathName(project)
            )
        )
        report.runtime = collectDependencies(project.configurations, GradleUtils.getRuntimeClasspathName(project))
        report.provided = collectDependencies(project.configurations, GradleUtils.getCompileOnlyName(project))
        report.test = collectDependencies(project.configurations, GradleUtils.getTestRuntimeClasspathName(project))

        // named Test-Suite support
        GradleUtils.getOtherTestRuntimeNames(project).forEach { namedTestSuite ->
            report.test += collectDependencies(project.configurations, namedTestSuite)
        }

        val developmentArtifacts = mutableListOf<Artifact>()
        developmentArtifacts.addAll(
            collectDependencies(project.configurations, JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME) +
                    createArtifact("org.gradle:gradle:" + project.gradle.gradleVersion, "sdk")
        )

        val cleanCodeReport = File(File(project.buildDir, "cleancode"), "cleancode-report.json")
        if (cleanCodeReport.exists()) {
            report.cleanCodeReport = CleanCodeReport.readFromFile(cleanCodeReport)
        }
        if (licenseFile.get().asFile.exists()) {
            report.licenseRecords = LicenseRecord.readFromStream(licenseFile.get().asFile.inputStream())
        }

        addDependenciesFromBuildscript(project, developmentArtifacts)
        // TODO we need to access the settings.buildScript as well

        val teamcityDslVersion = System.getenv("CLOUDFLIGHT_TEAMCITY_DSL")
        if (teamcityDslVersion != null && teamcityDslVersion.length > 0) {
            developmentArtifacts.add(
                createArtifact(
                    "io.cloudflight.devops.teamcity:teamcity-dsl:" + teamcityDslVersion,
                    "jar"
                )
            )
        }

        val packageJson = getPackageJson()
        val packageLockJson = getPackageLockJson()

        if (packageJson.isPresent && packageJson.get().asFile.exists() && packageLockJson.isPresent && packageLockJson.get().asFile.exists()) {
            try {
                val parser = NpmPackageParser()
                val npmModuleDependencies =
                    parser.parseNpmModule(packageJson.get().asFile, packageLockJson.get().asFile)
                compileArtifacts.addAll(npmModuleDependencies.compile)
                developmentArtifacts.addAll(npmModuleDependencies.development)
            } catch (ex: Exception) {
                project.logger.error("Error at parsing npm modules", ex)
            }
        }
        report.compile = compileArtifacts
        report.development = developmentArtifacts

        outputFile.get().asFile.outputStream().use {
            json.encodeToStream(report, it)
        }
    }

    private fun addDependenciesFromBuildscript(
        project: org.gradle.api.Project,
        developmentArtifacts: MutableList<Artifact>
    ) {
        project.buildscript.configurations.forEach { buildConf ->
            developmentArtifacts.addAll(
                collectDependencies(
                    project.buildscript.configurations,
                    buildConf.name
                ).filter { developmentArtifacts.find { a -> a.artifact == it.artifact } == null })
        }

        project.parent?.let { parent -> addDependenciesFromBuildscript(parent, developmentArtifacts) }
    }

    private fun createArtifact(artifact: String, type: String): Artifact {
        return Artifact(artifact, null, type, emptyList())
    }

    private fun collectDependencies(
        configurationContainer: ConfigurationContainer,
        configurationName: String
    ): List<Artifact> {
        val artifactList = mutableListOf<Artifact>()
        val configuration = configurationContainer.findByName(configurationName)
        if (configuration != null && configuration.isCanBeResolved) {
            addChildren(artifactList, configuration.incoming.resolutionResult.root, emptyList())
        }
        return artifactList
    }

    @Suppress("NestedBlockDepth")
    private fun addChildren(artifactList: MutableList<Artifact>, root: ResolvedComponentResult, trail: List<String>) {
        for (d in root.getDependencies()) {
            if (d is ResolvedDependencyResult) {
                val selected = d.getSelected()
                val module = selected.moduleVersion.toString()
                if (trail.isEmpty() /* root artifacts are always added */ || isNewArtifact(artifactList, module)) {

                    // unfortunately we cannot simply do def categoryAttribute = Category.CATEGORY_ATTRIBUTE here, gradle api sucks
                    val categoryAttribute = d.requested.attributes.keySet().firstOrNull { attr ->
                        attr.name == Category.CATEGORY_ATTRIBUTE.name
                    }
                    val categoryValue = categoryAttribute?.let { d.requested.attributes.getAttribute(it) }
                    var artifactType: String?
                    if (categoryValue == Category.REGULAR_PLATFORM || categoryValue == Category.ENFORCED_PLATFORM) {
                        artifactType = "bom"
                    } else {
                        artifactType = "jar"
                    }

                    val artifact = Artifact(module, null, artifactType, trail)

                    val existing = getExistingArtifact(artifactList, module)
                    if (existing == null) {
                        artifactList.add(artifact)

                        val subTrail = mutableListOf<String>()
                        subTrail.addAll(trail)
                        subTrail.add(module)
                        addChildren(artifactList, selected, subTrail)
                    } else {
                        artifactList.remove(existing) // remove the transitive dependency
                        artifactList.add(artifact) // and add the one without trail
                    }
                }
            }
        }
    }

    private fun getExistingArtifact(artifactList: List<Artifact>, module: String): Artifact? {
        for (artifact in artifactList) {
            if (artifact.artifact == module) {
                return artifact
            }
        }
        return null
    }

    private fun isNewArtifact(artifactList: List<Artifact>, module: String): Boolean {
        return getExistingArtifact(artifactList, module) == null
    }

    private fun toTrackerProject(project: org.gradle.api.Project): Project {
        var packaging = "jar"
        // TODO is there any better approach for that?
        if (project.pluginManager.hasPlugin("java-platform")) {
            packaging = "bom"
        }
        // TODO what about war's and so forth

        val artifact = project.group.toString() + ":" + project.name + ":" + project.version

        return Project(artifact, packaging, null)
    }
}