package com.ist.pharmacist.data

import com.ist.pharmacist.database.MedicineCacheData


data class Medicine(
    val name: String,
    var box_photo_path: String,
    val barcode: String,
    val purpose_preference: String,
    var users_to_notify: MutableList<String> = mutableListOf(),
    var pharmacy_ids: MutableList<String> = mutableListOf()
) {
    fun addUserToNotify(username: String) {
        users_to_notify.add(username)
    }

    fun removeUserToNotify(username: String) {
        users_to_notify.remove(username)
    }

    fun isUserToNotify(username: String): Boolean {
        return users_to_notify.contains(username)
    }

    fun addPharmacyId(pharmacyId: String) {
        pharmacy_ids.add(pharmacyId)
    }

    fun removePharmacyId(pharmacyId: String) {
        pharmacy_ids.remove(pharmacyId)
    }

    fun toMedicineCacheData(): MedicineCacheData {
        return MedicineCacheData(
            barcode = this.barcode,
            name = this.name,
            box_photo_path = this.box_photo_path,
            purpose_preference = this.purpose_preference,
            pharmacy_ids = this.pharmacy_ids,
            users_to_notify = this.users_to_notify
        )
    }
}