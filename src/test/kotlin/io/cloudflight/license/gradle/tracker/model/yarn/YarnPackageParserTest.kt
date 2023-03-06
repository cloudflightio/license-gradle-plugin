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
            .anyMatch { it.artifact == "@angular:animations:14.2.10" }
            .anyMatch { it.artifact == "@npm:tslib:2.4.1" }
            .noneMatch { it.artifact == "@angular-devkit:build-angular" }

        assertThat(module.development)
            .anyMatch { it.artifact.contains("@angular-devkit:build-angular:14.2.9") }

    }
}