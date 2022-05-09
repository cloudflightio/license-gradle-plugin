package io.cloudflight.license.gradle.tracker.task

import io.cloudflight.license.gradle.tracker.model.client.TrackerReportClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.IOException

abstract class SendToTrackerTask : DefaultTask() {

    @get:InputFile
    abstract val trackerFile: RegularFileProperty

    @get:Input
    abstract val trackerUrl: Property<String>

    @TaskAction
    fun sendToTracker() {
        try {
            val client = TrackerReportClient.getInstance(trackerUrl.get(), null)
            client.sendReport(trackerFile.get().asFile)
        } catch (e: IOException) {
            throw GradleException("Error in Tracker: " + e.message, e)
        }
    }
}