package io.cloudflight.license.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPlugin
import java.io.File
import java.util.zip.ZipFile

object GradleUtils {
    fun findRuntimeProjectDependencies(project: Project): List<Project> {
        return project.findRuntimeProjectDependencies()
    }

    fun hasLicenseFile(file: File): Boolean {
        val zip = ZipFile(file)
        val entry = zip.getEntry("META-INF/NOTICE.html")
        return entry != null
    }
}

fun Project.findRuntimeProjectDependencies() =
    configurations
        .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .allDependencies
        .filterIsInstance<ProjectDependency>()
        .map { it.dependencyProject }