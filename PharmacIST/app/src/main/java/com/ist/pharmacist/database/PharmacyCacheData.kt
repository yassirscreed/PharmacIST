package com.ist.pharmacist.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity( tableName = "pharmacies")
data class PharmacyCacheData(
    @PrimaryKey var id : String = "",
    var ownerName : String,
    var name : String,
    var address : String,
    var location : LatLng,
    var photo_path : String,
    var medicines : MutableMap<String, Int> = mutableMapOf(),
    var flaggers : MutableList<String> = mutableListOf(),
    var flaggedGlobally : Boolean = false
)


