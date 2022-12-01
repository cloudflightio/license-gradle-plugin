package io.cloudflight.license.gradle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LicensesTest {

    /**
     * The identifier is only used for logging in production ans is not relevant here in these tests
     */
    private var identifier = "io.cloudflight:gradle:1.0"

    @Test
    fun `MIT license with url`() {
        val license = Licenses.license(identifier, "MIT", null)
        assertEquals("https://opensource.org/licenses/MIT", license.licenseUrl)
    }

    @Test
    fun `MIT license without url`() {
        val license = Licenses.license(identifier, "MIT", "https://my-custom-mit-license")
        assertEquals("https://my-custom-mit-license", license.licenseUrl)
    }

    @Test
    fun `BSD license with url`() {
        val license = Licenses.license(identifier, "BSD 3-clause New License", null)
        assertEquals("https://opensource.org/licenses/BSD-3-Clause#Default", license.licenseUrl)
    }

    @Test
    fun `BSD license without url`() {
        val license = Licenses.license(identifier, "BSD 3-clause New License", "https://my-custom-mit-license")
        assertEquals("https://my-custom-mit-license", license.licenseUrl)
    }

    @Test
    fun `Apache license url overridden`() {
        val license = Licenses.license(identifier, "Apache-2.0", "https://my-fake-apache")
        assertEquals("https://www.apache.org/licenses/LICENSE-2.0", license.licenseUrl)
        assertEquals("Apache-2.0", license.licenseId)
    }

    @Test
    fun `0BSD license`() {
        val license = Licenses.license(identifier, "0BSD", null)
        assertNotNull(license)
    }
}