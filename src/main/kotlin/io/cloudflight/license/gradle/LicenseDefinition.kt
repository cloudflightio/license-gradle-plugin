package io.cloudflight.license.gradle

import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.Project
import java.io.File

@kotlinx.serialization.Serializable
class LicenseDefinition(
    val artifact: String,
    val license: String? = null,
    @SerialName("license_id") val licenseId: String? = null,
    @SerialName("license_url") val licenseUrl: String? = null
)

class LicenseDefinitionReader {

    private val json = Json {}

    fun loadLicenseOverrides(project: Project): List<LicenseDefinition> {
        val result = mutableListOf<LicenseDefinition>()
        collectLicenseOverrides(project, result)
        return result.toList()
    }

    private fun collectLicenseOverrides(project: Project, result: MutableList<LicenseDefinition>) {
        val licenseJson = File(project.projectDir, "licenses.json")
        if (licenseJson.exists()) {
            result.addAll(
                json.decodeFromStream(
                    ListSerializer(LicenseDefinition.serializer()), licenseJson.inputStream()
                )
            )
        }
        project.parent?.let {
            collectLicenseOverrides(it, result)
        }
    }

}
