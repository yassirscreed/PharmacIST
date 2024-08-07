package com.ist.pharmacist.data

import com.google.android.gms.maps.model.LatLng
import com.ist.pharmacist.database.PharmacyCacheData

data class Pharmacy(
    var id : String = "",
    var ownerName : String,
    var name : String,
    var address : String,
    var location : LatLng,
    var photo_path : String,
    var medicines : MutableMap<String, Int> = mutableMapOf(),
    var flaggers : MutableList<String> = mutableListOf(),
    var flaggedGlobally : Boolean = false

){
    fun addNewMedicine(barcode : String, quantity : Int) {
        medicines[barcode] = quantity
    }

    fun setPhotoPath(path: String) {
        photo_path = path
    }

    fun getMedicineQuantity(barcode: String): Int? {
        return medicines[barcode]
    }

    fun addMedicineQuantity(barcode: String, quantity: Int) {
        val currentQuantity = medicines[barcode] ?: 0
        medicines[barcode] = currentQuantity + quantity
    }

    fun removeMedicineQuantity(barcode: String, quantity: Int) : Boolean {
        val currentQuantity = medicines[barcode] ?: 0
        if (currentQuantity < quantity) {
            return false
        } else {
            medicines[barcode] = currentQuantity - quantity
            return true
        }
    }

    fun removeMedicine(barcode: String) {
        medicines.remove(barcode)
    }

    fun addFlagger(userId: String) {
        flaggers.add(userId)
    }

    fun removeFlagger(userId: String) {
        flaggers.remove(userId)
    }

    fun setflagPharmacy(flag: Boolean) {
        flaggedGlobally = flag
    }

    fun isFlagged(): Boolean {
        return flaggedGlobally
    }

    fun flagPharmacy() {
        flaggedGlobally = true
    }

    fun toPharmacyCacheData(): PharmacyCacheData {
        return PharmacyCacheData(
            id = this.id,
            ownerName = this.ownerName,
            name = this.name,
            address = this.address,
            location = this.location,
            photo_path = this.photo_path,
            medicines = this.medicines,
            flaggers = this.flaggers,
            flaggedGlobally = this.flaggedGlobally
        )
    }


}
