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

// DataStore for storing medicine photo paths
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medicine_preferences")
// Class to store and retrieve medicine photo paths
class MedicinePreferences(private val context: Context) {

    private val gson = Gson()

    suspend fun saveMedicinePhotoPath(barcode: String, path: String) {
        context.dataStore.edit { preferences ->
            val pathsJson = preferences[MEDICINE_PHOTO_PATH] ?: "{}"
            val pathsMap: MutableMap<String, String> = gson.fromJson(pathsJson, object: com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type)
            pathsMap[barcode] = path
            preferences[MEDICINE_PHOTO_PATH] = gson.toJson(pathsMap)
        }
    }
    suspend fun getMedicinePhotoPath(barcode: String): String? {
        return context.dataStore.data.firstOrNull()?.let {preferences ->
            val pathsJson = preferences[MEDICINE_PHOTO_PATH] ?: "{}"
            val pathsMap: Map<String, String> = gson.fromJson(pathsJson, object: com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type)
            pathsMap[barcode]
        }
    }

    suspend fun hasMedicinePhoto(pharmacyId: String): Boolean {
        val photoPath = getMedicinePhotoPath(pharmacyId)
        return photoPath != null && File(photoPath).exists()
    }

    companion object {
        val MEDICINE_PHOTO_PATH = stringPreferencesKey("medicine_photo_path")
    }
}