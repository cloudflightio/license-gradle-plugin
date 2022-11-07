package io.cloudflight.license.gradle.pom

import groovy.namespace.QName
import groovy.util.Node
import groovy.xml.XmlParser
import io.cloudflight.jsonwrapper.license.LicenseEntry
import io.cloudflight.license.gradle.Licenses
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.logging.Logging
import java.io.File

interface PomFileResolver {

    fun resolve(identifier: ModuleVersionIdentifier): File?

}

internal class PomParser(private val pomFileResolver: PomFileResolver) {

    private val xmlParser = XmlParser(false, false)

    fun parse(resolvedArtifact: ResolvedArtifact): PomFile {
        return PomFile(resolvedArtifact.moduleVersion.id, xmlParser.parse(resolvedArtifact.file), pomFileResolver, this)
    }

    fun parse(identifier: ModuleVersionIdentifier, file: File): PomFile {
        return PomFile(identifier, xmlParser.parse(file), pomFileResolver, this)
    }


    class PomFile internal constructor(
        private val identifier: ModuleVersionIdentifier,
        private val node: Node,
        private val resolver: PomFileResolver,
        private val parser: PomParser
    ) {

        val name = node.getName()
        val groupId = node.getGroupId()
        val artifactId = node.getArtifactId()
        val description = node.getDescription()
        val url = node.getUrl()
        val inceptionYear = node.getInceptionYear()

        fun findOrganizationName(): String? {
            val orgName = (node.getOrganizationName().firstOrNull() as Node?)?.getName()
            if (orgName != null) {
                return orgName
            }

            return getParentNode(identifier, findParent())?.findOrganizationName()
        }

        fun findLicenses(): Set<LicenseEntry> {
            val licenses = mutableSetOf<LicenseEntry>()

            licenses += node
                .getLicenses()
                .map { it as Node }
                .map { Licenses.license(identifier, it.getName(), it.getUrl()) }

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                licenses += parent.findLicenses()
            }

            return licenses
        }

        private fun extractDeveloperName(node:Node):String? {
            if (node.getName().isNotBlank()) {
                return node.getName()
            }
            if (node.getOrganizationName().text().isNotBlank()) {
                return node.getOrganizationName().text()
            }
            return null
        }

        fun findDevelopers(): Set<String> {
            val developers = mutableSetOf<String>()

            developers += node
                .getDevelopers()
                .map { it as Node }
                .mapNotNull { extractDeveloperName(it) }

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                developers += parent.findDevelopers()
            }

            return developers
        }

        fun findVersion(): String {
            val version = node.getVersion()
            if (version.isNotEmpty()) return version

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                return parent.findVersion()
            }

            return ""
        }

        fun findGroupId(): String {
            val groupId = node.getGroupId()
            if (groupId.isNotEmpty()) return groupId

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                return parent.findGroupId()
            }

            return ""
        }

        fun findDescription(): String {
            val description = node.getDescription()
            if (description.isNotEmpty()) return description

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                return parent.findDescription()
            }

            return ""
        }

        fun findUrl(): String {
            val url = node.getUrl()
            if (url.isNotEmpty()) return url

            val parent = getParentNode(identifier, findParent())

            if (parent != null) {
                return parent.findUrl()
            }

            return ""
        }


        private fun findParent(): ModuleVersionIdentifier? {
            val parents = node
                .getParent()
                .map {
                    val parent = it as Node
                    DefaultModuleVersionIdentifier.newId(
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion()
                    )
                }
                .toMutableSet()

            assert(parents.size < 2) { "more than one parent found" }

            return parents.firstOrNull()
        }

        private fun getParentNode(
            identifier: ModuleVersionIdentifier,
            parentIdentifier: ModuleVersionIdentifier?
        ): PomFile? {
            if (parentIdentifier == null) return null

            val parent = resolver.resolve(parentIdentifier)

            if (parent == null) {
                LOG.error("parent '$parentIdentifier' of artifact '$identifier' cannot be resolved")
                return null
            }
            return parser.parse(parentIdentifier, parent)
        }

        private fun Node.getArtifactId() = getTextAt("artifactId")

        private fun Node.getDescription(): String = getTextAt("description")

        private fun Node.getDevelopers() = getAt(QName.valueOf("developers")).getAt("developer")

        private fun Node.getGroupId() = getTextAt("groupId")

        private fun Node.getInceptionYear() = getTextAt("inceptionYear")

        private fun Node.getOrganizationName() = getAt(QName.valueOf("organization"))

        private fun Node.getLicenses() = getAt(QName.valueOf("licenses")).getAt("license")

        private fun Node.getName() = getTextAt("name")

        private fun Node.getParent() = getAt(QName.valueOf("parent"))

        private fun Node.getUrl() = getTextAt("url")

        private fun Node.getVersion() = getTextAt("version")

        private fun Node.getTextAt(name: String) = getAt(QName.valueOf(name))?.text().orEmpty().trim()

        companion object {
            private val LOG = Logging.getLogger(PomFile::class.java)
        }
    }

}