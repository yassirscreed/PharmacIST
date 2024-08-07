package com.ist.pharmacist.ui.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class MarkerLocationsViewModel : ViewModel() {

    private val _markerLocations = MutableLiveData<List<LatLng>>(emptyList())

    val markerLocations: LiveData<List<LatLng>> = _markerLocations


    fun addMarkerLocation(latLng: LatLng) {
        val currentList = _markerLocations.value.orEmpty().toMutableList()
        if(!currentList.contains(latLng))
            currentList.add(latLng)
        _markerLocations.value = currentList
    }

    fun containsLocation(latLng: LatLng): Boolean {
        return _markerLocations.value.orEmpty().contains(latLng)
    }

}