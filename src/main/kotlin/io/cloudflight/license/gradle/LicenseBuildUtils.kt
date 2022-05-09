package io.cloudflight.license.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants

object LicenseBuildUtils {

    const val DOCUMENTATION_ELEMENTS_CONFIGURATION_NAME = "documentationElements"

    fun createDocumentationConfiguration(project: Project): Configuration {
        val o = project.objects
        val configuration = project.configurations.maybeCreate(DOCUMENTATION_ELEMENTS_CONFIGURATION_NAME)
        configuration.apply {
            isVisible = false
            description = "Documentation and User Manuals"

            isCanBeResolved = false
            isCanBeConsumed = true

            with(attributes) {
                attribute(Usage.USAGE_ATTRIBUTE, o.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, o.named(Category::class.java, Category.DOCUMENTATION))
                attribute(Bundling.BUNDLING_ATTRIBUTE, o.named(Bundling::class.java, Bundling.EXTERNAL))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, o.named(DocsType::class.java, DocsType.USER_MANUAL))
            }
        }

        val component = project.components.getByName("java") as AdhocComponentWithVariants
        component.addVariantsFromConfiguration(configuration) {
            it.mapToMavenScope("runtime")
            it.mapToOptional()
        }

        return configuration
    }

    /**
     * Executes the given closure immediately if the task exists on projects, otherwise, as soon as it is added
     */
    fun withTask(project: Project, name: String, closure: (task: Task) -> Unit) {
        val task = project.tasks.findByName(name)
        if (task != null) {
            closure(task)
        } else {
            project.tasks.whenTaskAdded {
                if (it.name == name) {
                    closure(it)
                }
            }
        }
    }
}