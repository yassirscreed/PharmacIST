package com.ist.pharmacist.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ist.pharmacist.database.MedicineRepository
import com.ist.pharmacist.database.PharmacyRepository

class PharmaciesViewModelFactory(
    private val pharmacyRepository: PharmacyRepository,
    private val medicineRepository: MedicineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PharmaciesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PharmaciesViewModel(medicineRepository, pharmacyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}