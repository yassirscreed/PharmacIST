package com.ist.pharmacist.ui.views

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class NewMarkerLocationViewModel : ViewModel() {
    val location: MutableLiveData<LatLng> = MutableLiveData()
}