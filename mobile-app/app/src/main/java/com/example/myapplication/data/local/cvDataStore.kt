package com.example.myapplication.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.cvDataStore by preferencesDataStore("cv_draft")

class CvDraftStore(private val context: Context) {
    companion object {
        private val KEY_JSON = stringPreferencesKey("fields_json")
        private val KEY_SAVED_AT = longPreferencesKey("saved_at")
    }

    suspend fun save(fields: Map<String, String>) {
        val json = JSONObject(fields as Map<*, *>).toString()
        context.cvDataStore.edit { prefs ->
            prefs[KEY_JSON] = json
            prefs[KEY_SAVED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun load(): Pair<Map<String, String>, Long?> {
        val prefs = context.cvDataStore.data.map { it }.first()
        val json = prefs[KEY_JSON] ?: return emptyMap<String, String>() to null
        val savedAt = prefs[KEY_SAVED_AT]
        val obj = JSONObject(json)
        val map = buildMap {
            obj.keys().forEach { k -> put(k, obj.optString(k, "")) }
        }
        return map to savedAt
    }

    suspend fun clear() {
        context.cvDataStore.edit { prefs ->
            prefs.clear()
        }
    }

}
