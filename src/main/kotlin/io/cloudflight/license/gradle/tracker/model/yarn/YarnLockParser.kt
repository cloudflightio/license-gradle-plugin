package io.cloudflight.license.gradle.tracker.model.yarn

import org.yaml.snakeyaml.Yaml
import java.io.File

@Suppress("UNCHECKED_CAST")
class YarnLockParser(private val lockFile: File) {

    fun getDependencies(): Map<String, YarnLockEntry> {
        val yaml = Yaml();
        val map: Map<String, Map<String, Any>> = yaml.load(lockFile.inputStream())
        val result = mutableMapOf<String, YarnLockEntry>()
        map.forEach { (key, entry) ->
            key.split(", ").forEach { a ->
                result[a] = YarnLockEntry(
                    version = entry.getValue("version").toString(),
                    dependencies = entry.get("dependencies") as Map<String, Any>? ?: emptyMap(),
                )
            }
        }
        return result.toMap()
    }

    data class YarnLockEntry(
        val version: String,
        val dependencies:Map<String, Any>
    )

}