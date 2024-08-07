package com.ist.pharmacist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import com.google.gson.Gson
import java.io.File

// DataStore for storing pharmacy photo paths
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pharmacy_preferences")

// Class to store and retrieve pharmacy photo paths
class PharmacyPreferences(private val context: Context) {

    private val gson = Gson()

    suspend fun savePharmacyPhotoPath(pharmacyId: String, path: String) {
        context.dataStore.edit { preferences ->
            val pathsJson = preferences[PHARMACY_PHOTO_PATH] ?: "{}"
            val pathsMap: MutableMap<String, String> = gson.fromJson(pathsJson, object: com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type)
            pathsMap[pharmacyId] = path
            preferences[PHARMACY_PHOTO_PATH] = gson.toJson(pathsMap)
        }
    }
    suspend fun getPharmacyPhotoPath(pharmacyId: String): String? {
        return context.dataStore.data.firstOrNull()?.let {preferences ->
            val pathsJson = preferences[PHARMACY_PHOTO_PATH] ?: "{}"
            val pathsMap: Map<String, String> = gson.fromJson(pathsJson, object: com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type)
            pathsMap[pharmacyId]
        }
    }

    suspend fun hasPharmacyPhoto(pharmacyId: String): Boolean {
        val photoPath = getPharmacyPhotoPath(pharmacyId)
        return photoPath != null && File(photoPath).exists()
    }

    companion object {
        val PHARMACY_PHOTO_PATH = stringPreferencesKey("pharmacy_photo_path")
    }
}