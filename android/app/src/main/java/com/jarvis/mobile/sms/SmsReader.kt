package com.jarvis.mobile.sms

import android.content.Context
import android.net.Uri
import com.jarvis.mobile.data.PhoneSms

object SmsReader {
    fun read(context: Context, limit: Int = 20): List<PhoneSms> {
        return try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date", "read"),
                null, null, "date DESC"
            ) ?: return emptyList()
            val results = mutableListOf<PhoneSms>()
            cursor.use {
                var count = 0
                while (it.moveToNext() && count < limit) {
                    results.add(
                        PhoneSms(
                            from = it.getString(0) ?: "Unknown",
                            text = it.getString(1) ?: "",
                            time = it.getLong(2),
                            read = it.getInt(3) == 1
                        )
                    )
                    count++
                }
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }
}
