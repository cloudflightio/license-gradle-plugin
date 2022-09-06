package io.cloudflight.license.gradle.npm

import io.cloudflight.jsonwrapper.npm.NpmDependency
import io.cloudflight.jsonwrapper.npm.NpmPackageLock
import java.io.File

internal class NpmPackageLockUtils(private val packageLockFile: File) {

    private val packageLock = NpmPackageLock.readFromFile(packageLockFile)

    fun getDependencies(): Map<String, NpmDependency> {
        return packageLock.dependencies
    }

    fun getNpmModuleDirectory(it: Map.Entry<String, NpmDependency>): File {
        return File(packageLockFile.parentFile, "$NODE_MODULES/${it.key}")
    }

    companion object {
        private const val NODE_MODULES = "node_modules"
    }

}