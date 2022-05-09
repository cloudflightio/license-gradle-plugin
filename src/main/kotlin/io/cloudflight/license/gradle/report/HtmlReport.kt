package io.cloudflight.license.gradle.report

import io.cloudflight.jsonwrapper.license.LicenseEntry
import io.cloudflight.jsonwrapper.license.LicenseRecord
import kotlinx.html.*
import kotlinx.html.a
import kotlinx.html.stream.appendHTML

/**
 * Generates HTML report of projects dependencies.
 *
 * @property records list of [LicenseRecord]s for thr HTML report.
 */
class HtmlReport(private val records: List<LicenseRecord>) {

    /** Return Html as a String. */
    fun string(): String = if (records.isEmpty()) noOpenSourceHtml() else openSourceHtml()

    /** Html report when there are open source licenses. */
    private fun openSourceHtml(): String {
        val recordMap = hashMapOf<LicenseEntry, ArrayList<LicenseRecord>>()

        // Store packages by licenses: build a composite key of all the licenses, sorted in the (probably vain)
        // hope that there's more than one project with the same set of multiple licenses.
        records.forEach { record ->
            val keys = mutableListOf<LicenseEntry>()

            // first check to see if the record's license is in our list of known licenses.
            if (record.licenses.isNotEmpty()) {
                record.licenses.forEach { license ->
                    keys.add(license)
                }
            }

            record.licenses.forEach {
                recordMap.getOrPut(it, { arrayListOf() }).add(record)
            }
        }

        val sortedRecordMap = recordMap.toSortedMap(compareBy { it.license })

        return StringBuilder()
            .appendHTML()
            .html {
                head {
                    meta { charset = "utf-8" }
                    style {
                        unsafe { +CSS_STYLE }
                    }
                    title {
                        unsafe { +OPEN_SOURCE_LIBRARIES }
                    }
                }

                body {
                    h3 {
                        unsafe { +NOTICE_LIBRARIES }
                    }
                    ul {
                        sortedRecordMap.entries.forEach { entry ->
                            val sortedRecords = entry.value.sortedWith(
                                compareBy(String.CASE_INSENSITIVE_ORDER) { it.project }
                            )

                            var currentProject: LicenseRecord? = null
                            var currentLicense: Int? = null

                            sortedRecords.forEach { record ->
                                currentProject = record
                                currentLicense = entry.key.hashCode()

                                // Display libraries
                                li {
                                    a(href = "#$currentLicense") {
                                        +record.project
                                        +" (${record.version})"
                                    }
                                    val copyrightYear = if (record.year.isNullOrBlank()) DEFAULT_YEAR else record.year
                                    dl {
                                        if (record.developers.isNotEmpty()) {
                                            record.developers.forEach { developer ->
                                                dt {
                                                    +COPYRIGHT
                                                    +Entities.copy
                                                    +" $copyrightYear ${developer}"
                                                }
                                            }
                                        } else {
                                            dt {
                                                +COPYRIGHT
                                                +Entities.copy
                                                +" $copyrightYear $DEFAULT_AUTHOR"
                                            }
                                        }
                                    }
                                }
                            }

                            // This isn't correctly indented in the html source (but is otherwise correct).
                            // It appears to be a bug in the DSL implementation from what little I see on the web.
                            a(name = currentLicense.toString())

                            // Display associated license text with libraries
                            val licenses = currentProject?.licenses
                            if (licenses.isNullOrEmpty()) {
                                pre {
                                    unsafe { +NO_LICENSE }
                                }
                            } else {
                                licenses.forEach { license ->
                                    // if not found in the map, just display the info from the POM.xml
                                    val currentLicenseName = license.license
                                    val currentUrl = license.licenseUrl

                                    if (currentLicenseName.isNotEmpty() && currentUrl.isNotEmpty()) {
                                        pre {
                                            unsafe { +"$currentLicenseName\n<a href=\"$currentUrl\">$currentUrl</a>" }
                                        }
                                    } else if (currentUrl.isNotEmpty()) {
                                        pre {
                                            unsafe { +"<a href=\"$currentUrl\">$currentUrl</a>" }
                                        }
                                    } else if (currentLicenseName.isNotEmpty()) {
                                        pre {
                                            unsafe { +"$currentLicenseName\n" }
                                        }
                                    } else {
                                        pre {
                                            unsafe { +NO_LICENSE }
                                        }
                                    }
                                }
                                br
                            }
                            hr {}
                        }
                    }
                }
            }
            .toString()
    }

    /** Html report when there are no open source licenses. */
    private fun noOpenSourceHtml(): String = StringBuilder()
        .appendHTML()
        .html {
            head {
                style {
                    unsafe { +CSS_STYLE }
                }
                title {
                    +OPEN_SOURCE_LIBRARIES
                }
            }

            body {
                h3 {
                    unsafe { +NO_LIBRARIES }
                }
            }
        }.toString()

    companion object {
        const val CSS_STYLE =
            "body { font-family: sans-serif } pre { background-color: #eeeeee; padding: 1em; white-space: pre-wrap; display: inline-block }"
        const val OPEN_SOURCE_LIBRARIES = "Open source licenses"
        const val NO_LIBRARIES = "None"
        const val NO_LICENSE = "No license found"
        const val NOTICE_LIBRARIES = "Notice for packages:"
        const val COPYRIGHT = "Copyright "
        const val DEFAULT_AUTHOR = "The original author or authors"
        const val DEFAULT_YEAR = "20xx"
    }
}

@HtmlTagMarker
fun FlowOrInteractiveOrPhrasingContent.a(
    href: String? = null,
    target: String? = null,
    classes: String? = null,
    name: String? = null,
    block: A.() -> Unit = {}
): Unit = A(
    attributesMapOf(
        "href", href,
        "target", target,
        "class", classes,
        "name", name
    ),
    consumer
)
    .visit(block)
