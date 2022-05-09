package io.cloudflight.license.gradle

import io.cloudflight.jsonwrapper.license.LicenseEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

object CustomLicenses {
    private val licenseFile = LicenseEntry.readFromStream(
        CustomLicenses::class.java.classLoader.getResourceAsStream("licenses/custom/licenses.json")!!
    )
    private val synonomSerializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))
    private val licenseByDescription: Map<String, LicenseEntry>

    init {
        val map = mutableMapOf<String, LicenseEntry>()
        val licenseSynonyms = Json.decodeFromStream(
            synonomSerializer,
            CustomLicenses::class.java.classLoader
                .getResourceAsStream("licenses/custom/license-synonyms.json")!!
        )

        licenseFile.forEach {
            if (map.containsKey(it.license.lowercase())) {
                throw IllegalArgumentException("Duplicate License name ${it.license}")
            } else {
                map[it.license.lowercase()] = it
            }
        }
        licenseSynonyms.forEach { entry ->
            val license = licenseFile.firstOrNull { it.license == entry.key }
                ?: throw IllegalArgumentException("Unknown license ${entry.key} in license-synoyms.jso")
            entry.value.forEach { syn ->
                if (map.containsKey(syn.lowercase())) {
                    throw IllegalArgumentException("Duplicate License name ${syn}")
                } else {
                    map[syn.lowercase()] = license
                }
            }
        }
        licenseByDescription = map.toMap()
    }

    fun getByDescription(description: String): LicenseEntry? {
        return licenseByDescription[description.lowercase()]
    }
}