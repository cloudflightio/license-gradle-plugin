package io.cloudflight.license.gradle.task

import com.github.gradle.node.NodeExtension
import io.cloudflight.jsonwrapper.license.LicenseEntry
import io.cloudflight.jsonwrapper.license.LicenseRecord
import io.cloudflight.license.gradle.*
import io.cloudflight.license.gradle.npm.NpmLicenseParser
import io.cloudflight.license.gradle.pom.PomFileResolver
import io.cloudflight.license.gradle.pom.PomParser
import io.cloudflight.license.gradle.report.HtmlReport
import io.cloudflight.license.spdx.SpdxLicenses
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import java.io.File
import java.util.*

/**
 * A [Task] that creates HTML and JSON reports of the current projects dependencies.
 *
 * Modified version of https://github.com/jaredsburrows/gradle-license-plugin
 */
abstract class LicenseReportTask : DefaultTask() { // tasks can't be final
    private val json = Json { prettyPrint = true }
    private val npmLicenseParser = NpmLicenseParser()
    private val pomParser = PomParser(object : PomFileResolver {
        override fun resolve(identifier: ModuleVersionIdentifier): File? {
            val parent =
                resolveCompanionArtifacts(mutableMapOf(identifier to File(".")), null, POM_TYPE, true).firstOrNull()
            return parent?.file
        }
    })

    @get:OutputFile
    abstract val htmlFile: RegularFileProperty

    @get:OutputFile
    abstract val jsonFile: RegularFileProperty

    @InputFiles
    @Classpath
    fun getClasspath(): Collection<File> {
        return findRuntimeDependencies().values
    }

    @InputFiles
    fun getOtherModules(): List<File> {
        return project.findRuntimeProjectDependencies().map {
            File(
                it.buildDir,
                "licenses/license-report.json"
            )
        }
    }

    @InputFile
    @Optional
    fun getPackageLockJson(): Provider<RegularFile> {
        val node = project.extensions.findByType(NodeExtension::class.java)
        return if (node != null) {
            node.nodeProjectDir.file(NpmLicenseParser.PACKAGE_LOCK_JSON)
        } else {
            project.provider { null }
        }
    }

    private var knownLicenses = mapOf<ModuleVersionIdentifier, LicenseEntry>()

    @TaskAction
    fun licenseReport() {
        val dependencies = findRuntimeDependencies()

        val records = mutableSetOf<LicenseRecord>()

        records += findRuntimeProjects(dependencies)
        records += findLicenseReports(dependencies)
        records += findPomFiles(dependencies)
        if (getPackageLockJson().isPresent && getPackageLockJson().get().asFile.exists()) {
            records += npmLicenseParser.findNpmPackages(getPackageLockJson().get().asFile)
        }

        records += findUnresolvedLicenseOverrides(dependencies)

        // find*() above remove found artifacts from the dependencies collection, remaining dependencies are unresolved
        logUnresolvedDependencies(dependencies)

        val recordList = records.toList().sortedBy { it.project }

        createHtmlReport(recordList)
        createJsonReport(recordList)
    }

    fun setLicenseOverwrites(value: List<LicenseDefinition>) {
        val map = mutableMapOf<ModuleVersionIdentifier, LicenseEntry>()
        value.forEach {
            val identifier = Licenses.parseModuleVersionIdentifier(it.artifact)
            if (identifier != null) {
                if (it.licenseId != null) {
                    val spdxLicense = SpdxLicenses.findById(it.licenseId)
                    if (spdxLicense != null) {
                        map[identifier] = Licenses.license(identifier, spdxLicense.name, null)
                    } else {
                        LOG.error("The licenseId ${it.licenseId} is unknown")
                    }
                } else if (it.license != null && it.licenseUrl != null) {
                    // in case of a custom license directly from the build.gradle of a project, we do not want to log that error again
                    // TODO really?
                    map[identifier] = Licenses.license(identifier, it.license, it.licenseUrl, logUnknownLicense = false)
                } else {
                    LOG.error("Custom license definitions need to either have an id or a name and an URL")
                }
            }
        }
        knownLicenses = map.toMap()
    }

    private fun logUnresolvedDependencies(dependencies: MutableMap<ModuleVersionIdentifier, File>) {
        dependencies.forEach { Licenses.logMissingLicense(it.key) }
    }

    private fun findLicenseReports(
        dependencyArtifacts: MutableMap<ModuleVersionIdentifier, File>
    ): Collection<LicenseRecord> {
        val licenseRecords = mutableMapOf<ModuleVersionIdentifier, LicenseRecord>()

        val dependenciesWithLicenses = dependencyArtifacts
            .filter { GradleUtils.hasLicenseFile(it.value) }
            .toMutableMap()
        resolveCompanionArtifacts(dependenciesWithLicenses, REPORT_CLASSIFIER, REPORT_TYPE, false)
            .forEach { artifact ->
                try {
                    LicenceRecordReader.readFromFile(artifact.file)
                        .forEach { record ->
                            val identifier = Licenses.parseModuleVersionIdentifier(record.dependency)
                            if (identifier != null) {
                                licenseRecords[identifier] = record
                                dependencyArtifacts.remove(identifier)
                            }
                        }
                } catch (ex: Exception) {
                    LOG.debug("Could not parse ${artifact.file.absolutePath} from ${artifact.id.displayName}, transitive license information cannot be fetched")
                }
            }

        return licenseRecords.values
    }

    private fun findRuntimeProjects(dependencies: MutableMap<ModuleVersionIdentifier, File>): Collection<LicenseRecord> {
        val licenseRecords = mutableMapOf<ModuleVersionIdentifier, LicenseRecord>()

        val projectDependencies = project.findRuntimeProjectDependencies()
        projectDependencies.forEach {
            val jsonReport = File(
                it.buildDir,
                "licenses/license-report.json"
            ) // todo use the same naming schema here as in LicenseExtension, i.e. extract the relative part
            if (jsonReport.exists()) {
                LicenceRecordReader.readFromFile(jsonReport)
                    .forEach { record ->
                        val identifier = Licenses.parseModuleVersionIdentifier(record.dependency)
                        if (identifier != null) {
                            licenseRecords[identifier] =
                                record // TODO only if it is part of dependencies or it is an npm dependency
                            dependencies.remove(identifier)
                        }
                    }
            }
        }

        return licenseRecords.values
    }

    private fun resolveCompanionArtifacts(
        dependencies: MutableMap<ModuleVersionIdentifier, File>,
        classifier: String?,
        type: String,
        removeResults: Boolean
    ): List<ResolvedArtifact> {
        val configuration = project.configurations.create(type + UUID.randomUUID())

        dependencies
            .map { if (classifier != null) "${it.key}:$classifier@$type" else "${it.key}@$type" }
            .forEach { project.dependencies.add(configuration.name, it) }

        val artifacts = configuration
            .resolvedConfiguration
            .lenientConfiguration
            .artifacts
            .sortedBy { it.id.componentIdentifier.displayName }

        project.configurations.remove(configuration)

        if (removeResults) {
            artifacts.map { it.moduleVersion.id }.forEach { dependencies.remove(it) }
        }

        return artifacts
    }

    private fun findPomFiles(dependencies: MutableMap<ModuleVersionIdentifier, File>): List<LicenseRecord> {
        val artifacts = resolveCompanionArtifacts(dependencies, null, POM_TYPE, true)

        return artifacts
            .sortedBy { it.id.componentIdentifier.displayName }
            .map {
                val identifier = it.moduleVersion.id
                val node = pomParser.parse(it)

                val name = node.name
                val developers = node.findDevelopers()
                val licenses: Set<LicenseEntry> = extractLicenses(identifier, node)

                LicenseRecord(
                    dependency = it.moduleVersion.toString(),
                    version = node.findVersion(),
                    project = if (name.isNotEmpty()) name else node.artifactId,
                    description = node.description,
                    url = node.url,
                    year = node.inceptionYear,
                    licenses = licenses.toList(),
                    developers = developers.toList()
                )
            }
            .sortedBy { it.project }
    }

    private fun extractLicenses(identifier: ModuleVersionIdentifier, node: PomParser.PomFile): Set<LicenseEntry> {
        val license = knownLicenses[identifier]
        if (license != null) {
            return setOf(license)
        } else {
            val licenses = node.findLicenses()

            if (licenses.isEmpty()) {
                Licenses.logMissingLicense(identifier)
                return emptySet()
            } else if (licenses.size > 1) {
                // have a look if any of our preferred licenses can be found in the licenses
                PREFERRED_LICENSE_IDS.forEach { licenseId ->
                    val preferredLicense = licenses.firstOrNull { matchesLicense(it, licenseId) }
                    if (preferredLicense != null) {
                        Licenses.LOG_MULTIPLE.debug(
                            "Automatically chose ${preferredLicense.licenseId} out of " +
                                    "${licenses.joinToString { it.toString() }} for ${node.artifactId}"
                        )
                        return setOf(preferredLicense)
                    }
                }
                Licenses.logMultipleLicenses(identifier, licenses)
            }
            return licenses
        }
    }

    private fun matchesLicense(it: LicenseEntry, licenseId: String): Boolean {
        if (it.licenseId == licenseId) {
            return true
        }
        if (licenseId == Licenses.BSD && it.license == Licenses.BSD && it.licenseId == null) {
            // BSD licenses are Custom licenses, we don't treat them as SPDX licenses, and map them all together
            return true
        }
        return false
    }

    /**
     * All dependencies that could not have been resolved via any other variant, will get an entry here from the
     * licenses.json of all hardocded licenses.
     *
     * Problem here might be that this will make it impossible to override license entries from transitive files
     */
    private fun findUnresolvedLicenseOverrides(dependencies: MutableMap<ModuleVersionIdentifier, File>): Collection<LicenseRecord> {
        val records = mutableSetOf<LicenseRecord>()
        dependencies.toMap().forEach {
            if (knownLicenses.containsKey(it.key)) {
                val entry = knownLicenses.getValue(it.key)
                records.add(
                    LicenseRecord(
                        dependency = it.key.toString(),
                        version = it.key.version,
                        project = it.key.module.toString(),
                        description = null,
                        url = null,
                        year = null,
                        licenses = listOf(entry),
                        developers = emptyList()
                    )
                )
                dependencies.remove(it.key)
            }
        }
        return records
    }

    private fun findRuntimeDependencies() = project
        .configurations
        .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .resolvedConfiguration
        .lenientConfiguration
        .artifacts
        .filter { it.moduleVersion.id.group != project.group }
        .associateBy { it.moduleVersion.id }
        .mapValues { it.value.file }
        .toMutableMap()

    private fun createHtmlReport(records: List<LicenseRecord>) {
        val report = HtmlReport(records)
        createReport(htmlFile.get().asFile, report.string())
    }

    private fun createJsonReport(records: List<LicenseRecord>) {
        jsonFile.get().asFile.outputStream().use {
            json.encodeToStream(records, it)
        }
    }

    private fun createReport(file: File, content: String) {
        file.apply {
            // Remove existing file
            delete()

            // Create directories
            parentFile.mkdirs()
            createNewFile()

            // Write report for file
            bufferedWriter().use { it.write(content) }
        }
    }

    companion object {
        const val REPORT_TYPE = "json"
        const val REPORT_CLASSIFIER = "licenses"

        private const val POM_TYPE = "pom"

        private val LOG = Logging.getLogger(LicenseReportTask::class.java)

        /**
         * https://resources.whitesourcesoftware.com/blog-whitesource/license-compatibility
         * bzw. https://dwheeler.com/essays/floss-license-slide.pdf
         */
        private val PREFERRED_LICENSE_IDS = listOf(
            "MIT",
            "BSD",
            "CDDL-1.1",
            "CDDL-1.0",
            "Apache-2.0",
            "EPL-2.0",
            "EPL-1.0",
            "LGPL-3.0",
            "LGPL-2.1",
            "GPL-2.0-with-classpath-exception"
        )
    }
}
