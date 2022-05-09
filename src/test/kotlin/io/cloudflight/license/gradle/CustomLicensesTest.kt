package io.cloudflight.license.gradle

import io.cloudflight.license.gradle.CustomLicenses
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CustomLicensesTest {

    @Test
    fun findLicense() {
        assertEquals("BSD", CustomLicenses.getByDescription("BSD")?.license)
    }

    @Test
    fun findLicenseBySynonym() {
        assertEquals("BSD", CustomLicenses.getByDescription("BSD 3-clause New License")?.license)
    }

}