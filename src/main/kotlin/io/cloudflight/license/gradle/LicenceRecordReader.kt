package io.cloudflight.license.gradle

import io.cloudflight.jsonwrapper.license.LicenseRecord
import java.io.File

object LicenceRecordReader {

    fun readFromFile(file: File): List<LicenseRecord> {
        return LicenseRecord.readFromStream(file.inputStream())
    }
}