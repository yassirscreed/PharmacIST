package com.ist.pharmacist.ui.views

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPharmacyViewModel : ViewModel() {
    private val _pharmacyName = MutableStateFlow("")
    val pharmacyName: StateFlow<String> = _pharmacyName.asStateFlow()

    private val _confirmedImageUri = MutableStateFlow<Uri?>(null)
    val confirmedImageUri: StateFlow<Uri?> = _confirmedImageUri.asStateFlow()

    private val _showImageSelectionButtons = MutableStateFlow(true)
    val showImageSelectionButtons: StateFlow<Boolean> = _showImageSelectionButtons.asStateFlow()

    private val _confirmedImagePath = MutableStateFlow<String?>(null)
    val confirmedImagePath: StateFlow<String?> = _confirmedImagePath.asStateFlow()

    private val _tempPharmacyID = MutableStateFlow<String?>(null)
    val tempPharmacyID: StateFlow<String?> = _tempPharmacyID.asStateFlow()
    fun updatePharmacyName(name: String) {
        _pharmacyName.value = name
    }

    fun updateConfirmedImageUri(uri: Uri?) {
        _confirmedImageUri.value = uri
    }

    fun updateShowImageSelectionButtons(show: Boolean) {
        _showImageSelectionButtons.value = show
    }

    fun updateConfirmedImagePath(path: String?) {
        _confirmedImagePath.value = path
    }

    fun updateTempPharmacyID(id: String?) {
        _tempPharmacyID.value = id
    }


    fun uploadFileToFirebaseStorage() {

        viewModelScope.launch {
            // Get a reference to Firebase Storage
            val storage = FirebaseStorage.getInstance()

            // Create a reference to the file location
            val storageRef = storage.reference.child(_confirmedImagePath.value!!)

            // Upload the file
            storageRef.putFile(_confirmedImageUri.value!!)
                .addOnSuccessListener {
                    // File uploaded successfully
                    println("File uploaded successfully")
                }
                .addOnFailureListener { exception ->
                    // Handle unsuccessful uploads
                    println("File upload failed: ${exception.message}")
                }
        }
    }


}