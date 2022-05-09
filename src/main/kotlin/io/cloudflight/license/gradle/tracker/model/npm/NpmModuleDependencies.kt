package io.cloudflight.license.gradle.tracker.model.npm

import io.cloudflight.jsonwrapper.tracker.Artifact
import java.util.ArrayList

class NpmModuleDependencies {
    var compile: List<Artifact> = ArrayList()
    var development: List<Artifact> = ArrayList()
}