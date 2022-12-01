package io.cloudflight.license.gradle

import io.cloudflight.jsonwrapper.license.LicenseEntry
import io.cloudflight.license.spdx.LicenseQuery
import io.cloudflight.license.spdx.SpdxLicenses
import org.gradle.api.logging.Logging

object Licenses {

    private val LOG_MISSING = Logging.getLogger("io.cloudflight.gradle.license.missing")
    internal val LOG_MULTIPLE = Logging.getLogger("io.cloudflight.gradle.license.multiple")
    private val LOG_UNKNOWN = Logging.getLogger("io.cloudflight.gradle.license.unknown")

    // consult https://spdx.org/licenses/ for names and acronyms

    const val BSD = "BSD"
    private const val MIT = "MIT"

    internal fun logMissingLicense(identifier: String) {
        LOG_MISSING.error("'$identifier' does not provide any license information.")
    }

    internal fun logMissingUrl(artifact: String, name: String, url: String?) {
        LOG_MISSING.error("'$artifact' does not provide a valid license URL ($url) for '$name'.")
    }

    internal fun logMultipleLicenses(artifact: String, licenses: Set<LicenseEntry>) {
        LOG_MULTIPLE.error(
            "'$artifact' provides ambiguous licenses, choose the appropriate one:\n        ${
                licenses.joinToString("\n        ") { it.nameWithSpdxCode() }
            }"
        )
    }

    fun LicenseEntry.nameWithSpdxCode(): String {
        if (licenseId != null) {
            return "$license, licenceId = $licenseId"
        } else {
            return license
        }
    }

    internal fun logUnknownLicense(identifier: String, name: String, url: String?) {
        LOG_UNKNOWN.error("'$identifier' provides unknown license information. '$name' ($url) needs to be verified.")
    }

    internal fun license(
        artifact: String,
        name: String,
        url: String?,
        logUnknownLicense: Boolean = false
    ): LicenseEntry {
        val spdxLicense = SpdxLicenses.findLicense(LicenseQuery(name = name, url = url))
        if (spdxLicense != null) {
            if (spdxLicense.licenseId == MIT && !url.isNullOrBlank()) {
                return LicenseEntry(name, spdxLicense.licenseId, url)
            } else if (spdxLicense.seeAlso.isNotEmpty()) {
                return LicenseEntry(
                    spdxLicense.name,
                    spdxLicense.licenseId,
                    sanitizeUrl(spdxLicense.seeAlso.firstOrNull() ?: spdxLicense.reference)
                )
            } else if (!url.isNullOrBlank()) {
                return LicenseEntry(spdxLicense.name, spdxLicense.licenseId, sanitizeUrl(url))
            } else {
                logMissingUrl(artifact, name, url)
                return LicenseEntry(spdxLicense.name, spdxLicense.licenseId, "")
            }
        }
        val customLicense = CustomLicenses.getByDescription(name)
        if (customLicense != null) {
            if (customLicense.license == BSD && !url.isNullOrBlank()) {
                return LicenseEntry(customLicense.license, null, url)
            } else {
                return customLicense
            }
        }

        if (logUnknownLicense) logUnknownLicense(artifact, name, url)
        return LicenseEntry(name, null, url ?: "")
    }

    private fun sanitizeUrl(url: String): String {
        if (!url.startsWith("http")) {
            return "https://$url"
        } else {
            return url.replace("http://", "https://")
        }
    }
}
