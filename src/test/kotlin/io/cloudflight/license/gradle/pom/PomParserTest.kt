package io.cloudflight.license.gradle.pom

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File

class PomParserTest {

    private val parser = PomParser(object : PomFileResolver {
        override fun resolve(identifier: ModuleVersionIdentifier): File? {
            val file = pomFile(identifier.name, identifier.version)
            return if (file.exists()) {
                file
            } else {
                null
            }
        }
    })

    private fun pomFile(module: String, version: String): File {
        return File("src/test/resources/pom/${module}-${version}.pom")
    }

    private fun parse(group: String, module: String, version: String): PomParser.PomFile {
        return parser.parse(
            DefaultModuleVersionIdentifier.newId(group, module, version),
            pomFile(module, version)
        )
    }

    @Test
    fun parseFile() {
        val pom = parse("org.springframework.boot", "spring-boot", "2.3.0.RELEASE")
        assertEquals("Pivotal Software, Inc.", pom.findOrganizationName())
        assertEquals("spring-boot", pom.name)
    }

    @Test
    fun parseFileWithParent() {
        val pom = parse("com.fasterxml.jackson", "jackson-core", "2.11.3")

        assertEquals("Jackson-core", pom.name)
        assertEquals("2.11.3", pom.findVersion())
        assertEquals(1, pom.findLicenses().size)
        assertEquals("FasterXML", pom.findOrganizationName())
        val developers = pom.findDevelopers()
        assertEquals(3, developers.size)
        assertNotNull(developers.find { it == "Tatu Saloranta"})
    }

}