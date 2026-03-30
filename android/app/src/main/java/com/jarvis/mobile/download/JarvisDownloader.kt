package com.jarvis.mobile.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object JarvisDownloader {

    /**
     * Enqueue a file download to the public Downloads folder via Android's DownloadManager.
     * Shows a system notification with progress. No extra permissions needed on API 29+.
     */
    fun download(context: Context, url: String, filename: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Downloading via JARVIS")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
