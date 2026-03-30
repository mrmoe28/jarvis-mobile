package com.jarvis.mobile.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.mobile.data.PhoneNotification

class JarvisNotificationListener : NotificationListenerService() {

    companion object {
        val recent = ArrayDeque<PhoneNotification>()
        var isConnected = false
        private const val MAX_SIZE = 50
    }

    override fun onListenerConnected() {
        isConnected = true
    }

    override fun onListenerDisconnected() {
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) { sbn.packageName }

        synchronized(recent) {
            recent.addFirst(PhoneNotification(appName, title, text, sbn.postTime))
            while (recent.size > MAX_SIZE) recent.removeLast()
        }
    }
}
