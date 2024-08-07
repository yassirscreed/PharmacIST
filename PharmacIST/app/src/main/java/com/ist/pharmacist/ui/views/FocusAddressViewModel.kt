package com.ist.pharmacist.ui.views

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class FocusAddressViewModel : ViewModel() {
    val location: MutableLiveData<LatLng> = MutableLiveData()

    fun updateLocation(latLng: LatLng) {
        location.value = latLng
    }
}