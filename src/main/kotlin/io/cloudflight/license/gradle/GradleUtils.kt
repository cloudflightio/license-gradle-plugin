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
        return if (file.exists()) {
            val zip = ZipFile(file)
            val entry = zip.getEntry("META-INF/NOTICE.html")
            entry != null
        } else {
            false
        }
    }

    fun getRuntimeClasspathName(project: Project): String {
        return if (isAndroidProject(project)) {
            "releaseRuntimeClasspath"
            // currently, the following is still skipped:
            //   - debugRuntimeClasspath
        } else {
            JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
        }
    }

    fun getCompileClasspathName(project: Project): String {
        return if (isAndroidProject(project)) {
            "releaseCompileClasspath"
            // currently, the following is still skipped:
            //   - debugCompileClasspath
        } else {
            JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME
        }
    }

    fun getCompileOnlyName(project: Project): String {
        return if (isAndroidProject(project)) {
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
        } else {
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
        }
    }

    fun getTestRuntimeClasspathName(project: Project): String {
        return if (isAndroidProject(project)) {
            "releaseUnitTestRuntimeClasspath"
            // the other cases are covered by the Test-Suite edge case:
            //   - debugUnitTestRuntimeClasspath
            //   - debugAndroidTestRuntimeClasspath
        } else {
            JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME
        }
    }

    fun getOtherTestRuntimeNames(project: Project) = project.configurations.names.filter {
        it.lowercase().endsWith(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME.lowercase())
                && it != getRuntimeClasspathName(project) && it != getTestRuntimeClasspathName(project)
                && it.lowercase().contains("test")}

    private fun isAndroidProject(project: Project): Boolean {
        return project.configurations.names.contains("releaseRuntimeClasspath")
    }
}

fun Project.findRuntimeProjectDependencies() = configurations
        .getByName(GradleUtils.getRuntimeClasspathName(this))
        .allDependencies
        .filterIsInstance<ProjectDependency>()
        .map { it.dependencyProject }