package com.jarvis.mobile.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.mobile.data.SavedLogin

class LoginPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_logins",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    var logins: List<SavedLogin>
        get() {
            val json = prefs.getString("logins", "[]") ?: "[]"
            val type = object : TypeToken<List<SavedLogin>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
        private set(value) {
            prefs.edit().putString("logins", gson.toJson(value)).apply()
        }

    fun save(login: SavedLogin) {
        val current = logins.toMutableList()
        val idx = current.indexOfFirst { it.id == login.id }
        if (idx >= 0) current[idx] = login else current.add(login)
        logins = current
    }

    fun delete(id: String) {
        logins = logins.filter { it.id != id }
    }
}
