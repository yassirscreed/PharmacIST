package com.ist.pharmacist.ui.views

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel

class PhotoCacheViewModel : ViewModel() {
    private val _pharmacyPhotoCache = mutableStateMapOf<String, String>()
    val pharmacyPhotoCache: Map<String, String> = _pharmacyPhotoCache

    private val _medicinePhotoCache = mutableStateMapOf<String, String>()
    val medicinePhotoCache: Map<String, String> = _medicinePhotoCache

    fun cachePharmacyPhoto(pharmacyID: String, photoUrl: String) {
        _pharmacyPhotoCache[pharmacyID] = photoUrl
    }

    fun cacheMedicinePhoto(barcode: String, photoUrl: String) {
        _medicinePhotoCache[barcode] = photoUrl
    }
}