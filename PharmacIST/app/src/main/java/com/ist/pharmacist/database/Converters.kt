package com.ist.pharmacist.database

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class Converters {
    @TypeConverter
    fun fromStringToList(value: String): MutableList<String> {
        val listType = object : TypeToken<MutableList<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromMutableList(list: MutableList<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToMap(value: String): MutableMap<String, Int> {
        val mapType = object : TypeToken<MutableMap<String, Int>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMutableMap(map: MutableMap<String, Int>): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun fromLatLng(latLng: LatLng): String {
        return Gson().toJson(latLng)
    }

    @TypeConverter
    fun toLatLng(value: String): LatLng {
        return Gson().fromJson(value, LatLng::class.java)
    }
}