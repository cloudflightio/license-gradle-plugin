package io.cloudflight.license.gradle.tracker.model.yarn

import io.cloudflight.jsonwrapper.license.LicenseRecord
import io.cloudflight.jsonwrapper.npm.NpmDependency
import io.cloudflight.jsonwrapper.npm.NpmPackage.Companion.readFromFile
import io.cloudflight.jsonwrapper.npm.NpmUtils.getGavForNpmEntry
import io.cloudflight.jsonwrapper.tracker.Artifact
import io.cloudflight.license.gradle.tracker.model.npm.NpmModuleDependencies
import java.io.File
import java.io.IOException

internal object YarnPackageParser {

    fun findNpmPackages(packageJson: File, yarnLock: File): Collection<LicenseRecord> {
        val npmPackageFile = readFromFile(packageJson)
        val yarnLockParser = YarnLockParser(yarnLock)

        val dependencies = yarnLockParser.getDependencies()

        val modules = mutableSetOf<NodeModule>()

        getModules(packageJson.parentFile, npmPackageFile.dependencies, dependencies, modules)

        val licenseRecords = mutableListOf<LicenseRecord>()

        return licenseRecords.toList()
    }


    private data class NodeModule(val name: String, val directory: File)




    @Throws(IOException::class)
    fun parseNpmModule(packageJson: File, yarnLock: File): NpmModuleDependencies {
        val npmPackageFile = readFromFile(packageJson)
        val yarnLockParser = YarnLockParser(yarnLock)

        val dependencies = yarnLockParser.getDependencies()

        val result = NpmModuleDependencies()

        val compileDependencies = mutableListOf<Artifact>()
        val developmentDependencies = mutableListOf<Artifact>()

        addDependencies(npmPackageFile.dependencies, dependencies, compileDependencies)
        addDependencies(npmPackageFile.devDependencies, dependencies, developmentDependencies)

        result.compile = compileDependencies
        result.development = developmentDependencies
        return result
    }

    private fun addDependencies(
        packageJsonDependencies: Map<String, String>,
        lockFileResolution: Map<String, YarnLockParser.YarnLockEntry>,
        result: MutableList<Artifact>
    ) {
        packageJsonDependencies.forEach { d ->
            addArtifact(d.key, d.value, lockFileResolution, result, emptyList())
        }
    }

    private fun getModules(
        projectDirectory: File,
        packageJsonDependencies: Map<String, String>,
        lockFileResolution: Map<String, YarnLockParser.YarnLockEntry>,
        result:MutableSet<NodeModule>
    ) {

        packageJsonDependencies.forEach { d ->
            addModule(projectDirectory, d.key, d.value, lockFileResolution, result)
        }
    }

    private fun addModule(
        projectDirectory: File,
        packageName: String,
        declaredVersion: String,
        lockFileResolution: Map<String, YarnLockParser.YarnLockEntry>,
        result: MutableSet<NodeModule>
    ) {
        val identifier = if (declaredVersion.startsWith("npm:")) "$packageName@$declaredVersion" else "$packageName@npm:$declaredVersion"
        val lockInfo = lockFileResolution[identifier]
        if (lockInfo != null) {
            val version = lockInfo.version
            val gav = getGavForNpmEntry(mapOf(packageName to NpmDependency(version = version)).entries.first())
            if (result.count { it.name == gav } == 0) {
                val directory = File(projectDirectory, "node_modules/$packageName}")
                result.add(NodeModule(name = packageName, directory = directory))
                lockInfo.dependencies.forEach {
                    addModule(projectDirectory, it.key, it.value.toString(), lockFileResolution, result)
                }
            }
        } else {
            throw IllegalArgumentException(identifier)
        }
    }

    private fun addArtifact(
        packageName: String,
        declaredVersion: String,
        lockFileResolution: Map<String, YarnLockParser.YarnLockEntry>,
        result: MutableList<Artifact>, trail: List<String>
    ) {
        val identifier = if (declaredVersion.startsWith("npm:")) "$packageName@$declaredVersion" else "$packageName@npm:$declaredVersion"
        val lockInfo = lockFileResolution[identifier]
        if (lockInfo != null) {
            val version = lockInfo.version
            val gav = getGavForNpmEntry(mapOf(packageName to NpmDependency(version = version)).entries.first())
            if (result.count { it.artifact == gav } == 0) {
                result.add(Artifact(artifact = gav, type = "npm", classifier = null, trail = trail))
                lockInfo.dependencies.forEach {
                    addArtifact(it.key, it.value.toString(), lockFileResolution, result, trail + gav)
                }
            }
        } else {
            throw IllegalArgumentException(identifier)
        }
    }
}
