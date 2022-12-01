package io.cloudflight.license.gradle.npm

import com.github.gradle.node.NodeExtension
import io.cloudflight.jsonwrapper.license.LicenseEntry
import io.cloudflight.jsonwrapper.license.LicenseRecord
import io.cloudflight.jsonwrapper.npm.NpmPackage
import io.cloudflight.jsonwrapper.npm.NpmUtils
import io.cloudflight.license.gradle.Licenses
import org.gradle.api.logging.Logging
import java.io.File

class NpmLicenseParser {
    @Suppress("NestedBlockDepth")
    fun findNpmPackages(packageLockFile: File): Collection<LicenseRecord> {
        val projects = mutableListOf<LicenseRecord>()
        val packageLock = NpmPackageLockUtils(packageLockFile)
        packageLock.getDependencies().entries.forEach {
            if (!it.value.dev) {
                try {
                    val npmModuleDirectory: File = packageLock.getNpmModuleDirectory(it)
                    if (npmModuleDirectory.exists()) {
                        val npmPackage = File(npmModuleDirectory, PACKAGE_JSON)
                        if (npmPackage.exists()) {
                            val p = NpmPackage.readFromFile(npmPackage)
                            val gav = NpmUtils.getGavForNpmEntry(it)
                            projects.add(
                                LicenseRecord(
                                    project = p.name,
                                    version = p.version,
                                    url = p.homepage ?: "",
                                    description = p.description ?: "",
                                    licenses = findLicense(p, gav),
                                    developers = findDevelopersForNpm(p),
                                    dependency = gav,
                                    year = null
                                )
                            )
                        } else if (!it.value.optional) {
                            LOG.warn("$npmPackage does not exist, license can't be parsed")
                        }
                    } else if (!it.value.optional) {
                        LOG.error("${npmModuleDirectory.absoluteFile} does not exist, license can't be parsed")
                    }
                } catch (ex: Exception) {
                    LOG.error("Error while analyzing license of ${it.key}, see debug for more details")
                    // this happens from time to time as there are lots of crap npm libraries out there, and we don't want to bump or logs with it
                    LOG.debug("Error while parsing package.json of ${it.key}", ex)
                }
            }
        }
        return projects
    }

    private fun findDevelopersForNpm(p: NpmPackage): List<String> {
        val devs = mutableListOf<String>()
        if (p.author != null) {
            devs.add(p.author!!.name)
        }
        devs.addAll(p.contributors.filter { it.name != p.author?.name }.map { it.name })
        return devs
    }

    private fun findLicense(npmPackage: NpmPackage, artifact: String): List<LicenseEntry> {
        return if (npmPackage.license == null) {
            emptyList()
        } else {
            listOf(
                Licenses.license(
                    artifact,
                    npmPackage.license!!,
                    // package.json does not have license urls, and most of them are using the MIT license
                    // which we need to map manually here. the others are mapped in Licenses.kt
                    if (npmPackage.license == "MIT") "https://opensource.org/licenses/MIT" else null
                )
            )
        }
    }

    companion object {
        internal const val NODE_MODULES = "node_modules"
        const val PACKAGE_LOCK_JSON = "package-lock.json"
        const val PACKAGE_JSON = "package.json"

        private val LOG = Logging.getLogger(NpmLicenseParser::class.java)

        @JvmStatic
        fun getPackageJsonFile(node: NodeExtension) = node.nodeProjectDir.file(PACKAGE_JSON).get().asFile
    }
}
