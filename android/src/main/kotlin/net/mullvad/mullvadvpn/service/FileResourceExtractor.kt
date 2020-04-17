package net.mullvad.mullvadvpn.service

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class FileResourceExtractor(val context: Context) {
    fun extract(asset: String) {
        val destination = File(context.filesDir, asset)

        if (!destination.exists()) {
            extractFile(asset, destination)
        }
    }

    private fun extractFile(asset: String, destination: File) {
        val destinationStream = FileOutputStream(destination)

        context
            .assets
            .open(asset)
            .copyTo(destinationStream)

        destinationStream.close()
    }
}
