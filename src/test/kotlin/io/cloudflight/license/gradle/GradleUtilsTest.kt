package io.cloudflight.license.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class GradleUtilsTest {

    @Test
    fun hasLicense() {
        assertThat(GradleUtils.hasLicenseFile(File("src/test/resources/npm-parser-0.1.0.jar"))).isTrue()
    }

    @Test
    fun hasNoLicense() {
        assertThat(GradleUtils.hasLicenseFile(File("src/test/resources/apiguardian-api-1.1.0.jar"))).isFalse()
    }

}