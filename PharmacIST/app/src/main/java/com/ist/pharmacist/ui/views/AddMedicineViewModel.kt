package com.ist.pharmacist.ui.views

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddMedicineViewModel : ViewModel() {

    private val _medicineName = MutableStateFlow("")

    private val _quantity = MutableStateFlow("")

    private val _purpose = MutableStateFlow("")

    private val _barcode = MutableStateFlow("")

    val medicineName: StateFlow<String> = _medicineName.asStateFlow()

    val quantity: StateFlow<String>  = _quantity.asStateFlow()

    val purpose: StateFlow<String> = _purpose.asStateFlow()

    private val _confirmedImageUri = MutableStateFlow<Uri?>(null)
    val confirmedImageUri: StateFlow<Uri?> = _confirmedImageUri.asStateFlow()

    private val _showImageSelectionButtons = MutableStateFlow(true)
    val showImageSelectionButtons: StateFlow<Boolean> = _showImageSelectionButtons.asStateFlow()

    private val _confirmedImagePath = MutableStateFlow<String?>(null)
    val confirmedImagePath: StateFlow<String?> = _confirmedImagePath.asStateFlow()

    val barcode : StateFlow<String> = _barcode.asStateFlow()
    fun updateMedicineName(name: String) {
        _medicineName.value = name
    }

    fun updateQuantity(quantity: String) {
        _quantity.value = quantity
    }

    fun updatePurpose(name: String) {
        _purpose.value = name
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

    fun updateBarCode(barcode: String) {
        _barcode.value = barcode
    }

    fun getBarCode() : String {
        return _barcode.value
    }

    fun getQuantity() : String {
        return _quantity.value
    }


    fun uploadFileToFirebaseStorage() {

        viewModelScope.launch {
            Log.d("AddMedicineViewModel", "Uploading file to Firebase Storage")
            Log.d("AddMedicineViewModel", "Confirmed image path: ${_confirmedImagePath.value}")
            Log.d("AddMedicineViewModel", "Confirmed image URI: ${_confirmedImageUri.value}")

            // Get a reference to Firebase Storage
            val storage = FirebaseStorage.getInstance()

            // Use safe call operator and let function to handle nullable values
            _confirmedImagePath.value?.let { imagePath ->
                _confirmedImageUri.value?.let { imageUri ->
                    // Create a reference to the file location
                    val storageRef = storage.reference.child(imagePath)

                    // Check if the file already exists in Firebase Storage
                    storageRef.getMetadata()
                        .addOnSuccessListener {
                            // File exists, no need to upload again
                            Log.d("AddMedicineViewModel", "File already exists in Firebase Storage")
                        }
                        .addOnFailureListener { exception ->
                            // File doesn't exist, proceed with the upload
                            if (exception is StorageException && exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                // Upload the file
                                storageRef.putFile(imageUri)
                                    .addOnSuccessListener {
                                        // File uploaded successfully
                                        Log.d("AddMedicineViewModel", "File uploaded successfully")
                                    }
                                    .addOnFailureListener { uploadException ->
                                        // Handle unsuccessful uploads
                                        Log.e("AddMedicineViewModel", "File upload failed: ${uploadException.message}")
                                    }
                            } else {
                                // Handle other errors
                                Log.e("AddMedicineViewModel", "Error checking file existence: ${exception.message}")
                            }
                        }
                } ?: run {
                    // Handle the case when _confirmedImageUri is null
                    Log.e("AddMedicineViewModel", "Confirmed image URI is null. Cannot upload file.")
                }
            } ?: run {
                // Handle the case when _confirmedImagePath is null
                Log.e("AddMedicineViewModel", "Confirmed image path is null. Cannot upload file.")
            }
        }
    }
}