package io.cloudflight.license.gradle.tracker.model.yarn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class YarnPackageParserTest {

    @Test
    fun parseFile() {
        val module = YarnPackageParser.parseNpmModule(
            File("src/test/resources/yarn/package.json"),
            File("src/test/resources/yarn/yarn.lock")
        )

        assertThat(module.compile)
            .anyMatch { it.artifact == "@angular:animations:16.2.8" }
            .anyMatch { it.artifact == "@npm:tslib:2.6.2" }
            .noneMatch { it.artifact == "@angular-devkit:build-angular" }

        assertThat(module.development)
            .anyMatch { it.artifact.contains("@angular-devkit:build-angular:16.2.5") }
    }

    @Test
    fun findNpmPackages() {
        val licenses = YarnPackageParser.findNpmPackages(
            File("src/test/resources/yarn/package.json"),
            File("src/test/resources/yarn/yarn.lock")
        )

        // parsing licenses is not implemented yet
        // this test still provides value by testing the parser
        assertThat(licenses).isEmpty();
    }
}
