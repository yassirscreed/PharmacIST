package com.ist.pharmacist.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity( tableName = "medicines")
data class MedicineCacheData(
    val name: String,
    var box_photo_path: String,
    @PrimaryKey val barcode: String,
    val purpose_preference: String,
    val users_to_notify: MutableList<String> = mutableListOf(),
    val pharmacy_ids: MutableList<String> = mutableListOf()
)
