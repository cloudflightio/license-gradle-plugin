package io.cloudflight.license.gradle.tracker.model.npm;

import io.cloudflight.jsonwrapper.npm.NpmDependency;
import io.cloudflight.jsonwrapper.npm.NpmPackage;
import io.cloudflight.jsonwrapper.npm.NpmPackageLock;
import io.cloudflight.jsonwrapper.npm.NpmUtils;
import io.cloudflight.jsonwrapper.tracker.Artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NpmPackageParser {

    /**
     * Parses the file <code>package-lock.json</code>
     *
     * @return the dependencies of that module
     * @throws IOException if the file does not exist or cannot be parsed
     */
    public NpmModuleDependencies parseNpmModule(File packageJson, File packageJsonLock) throws IOException {
        NpmPackage npmPackageFile = NpmPackage.Companion.readFromFile(packageJson);
        NpmPackageLock npmPackageLock = NpmPackageLock.Companion.readFromFile(packageJsonLock);

        try {
            NpmModuleDependencies dependencies = new NpmModuleDependencies();
            for (Map.Entry<String, NpmDependency> entry : npmPackageLock.getDependencies().entrySet()) {
                Artifact a = new Artifact(
                        NpmUtils.INSTANCE.getGavForNpmEntry(entry),
                        null,
                        "npm",
                        getTrail(entry, npmPackageFile, npmPackageLock)
                );
                // TODO exclude optional dependencies if they have not been resolved (i.e. they are not inside the node_modules folder)
                if (entry.getValue().getDev()) {
                    dependencies.getDevelopment().add(a);
                } else {
                    dependencies.getCompile().add(a);
                }
            }

            return dependencies;
        } catch (Exception ex) {
            throw new IOException("package.json could not be read", ex);
        }
    }

    private List<String> getTrail(Map.Entry<String, NpmDependency> entry, NpmPackage npmPackageFile, NpmPackageLock npmPackageLock) {
        if (entry.getValue().getDev()) {
            return getTrail(npmPackageFile.getDevDependencies().keySet(), npmPackageFile, npmPackageLock, entry.getKey());
        } else {
            return getTrail(npmPackageFile.getDependencies().keySet(), npmPackageFile, npmPackageLock, entry.getKey());
        }
    }

    private List<String> getTrail(Set<String> nonTransitiveDependencies, NpmPackage npmPackageJson, NpmPackageLock npmPackageLock, String module) {
        if (nonTransitiveDependencies.contains(module)) {
            return null;
        }
        return collectTrail(new ArrayList<>(), module, nonTransitiveDependencies, npmPackageLock);
    }

    private List<String> collectTrail(List<String> trail, String module, Set<String> possiblePaths, NpmPackageLock npmPackageLock) {
        if (possiblePaths.contains(module)) {
            return trail;
        }
        for (String possiblePath : possiblePaths) {
            if (!trail.contains(possiblePath)) {  // avoid cycles
                List<String> subTrail = new ArrayList<>(trail);
                subTrail.add(possiblePath);
                NpmDependency npmDependency = npmPackageLock.getDependencies().get(possiblePath);
                if (npmDependency != null) {
                    List<String> collectedSubTrail = collectTrail(subTrail, module, npmDependency.getRequires().keySet(), npmPackageLock);
                    if (collectedSubTrail != null) {
                        return collectedSubTrail;
                    }
                } else {
                    // This is a really rare case and it looks like a fuckup of npm package-lock.json, it happens
                    // if modules have subdependencies that are not tracked down to the root dependencies
                    // TODO support that as well
                }
            }
        }
        return null;
    }
}
