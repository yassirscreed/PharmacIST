package com.ist.pharmacist.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ist.pharmacist.data.Medicine
import com.ist.pharmacist.data.Pharmacy
import com.ist.pharmacist.data.User
import com.ist.pharmacist.database.MedicineCacheData
import com.ist.pharmacist.database.MedicineRepository
import com.ist.pharmacist.database.PharmacyCacheData
import com.ist.pharmacist.database.PharmacyRepository
import com.ist.pharmacist.utils.calculateBounds
import com.ist.pharmacist.utils.calculateDistance
import com.ist.pharmacist.utils.extractMedicineFromDocumentData
import com.ist.pharmacist.utils.extractPharmacyFromDocumentData
import com.ist.pharmacist.utils.extractUserFromDocumentData
import com.ist.pharmacist.utils.getCurrentLocation
import com.ist.pharmacist.utils.isMeteredConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PharmaciesViewModel(
    private val medicineRepository: MedicineRepository,
    private val pharmacyRepository: PharmacyRepository
) : ViewModel() {

    private val _pharmacies = MutableLiveData<Map<String, Pharmacy>>(emptyMap())

    private val _medicines = MutableLiveData<Map<String, Medicine>>(emptyMap())

    private val _medicineBarcodes = MutableLiveData<List<String>>(emptyList())

    private val _addMedicine = MutableStateFlow(false)

    val addMedicine = _addMedicine.asStateFlow()

    private val _removeMedicine = MutableStateFlow(false)

    val removeMedicine = _removeMedicine.asStateFlow()

    private val _flagPharmacyMenu = MutableStateFlow(false)

    val flagPharmacyMenu = _flagPharmacyMenu.asStateFlow()


    ////////////////////////////////List of Pharmacies and Medicines Handler///////////////////////////////////////////////
    fun addPharmacy(pharmacy: Pharmacy) {
        val currentMap = _pharmacies.value.orEmpty().toMutableMap()
        currentMap[pharmacy.id] = pharmacy
        _pharmacies.value = currentMap
    }

    fun addPharmacyToListOfPharmaciesCreatedByUser(pharmacyId: String, user: User) {
        user.addPharmacyCreatedByUser(pharmacyId)
        // Update the user's pharmacies in the backend
        updateUserInBackend(user)
    }

    fun addMedicine(medicine: Medicine) {
        val currentMap = _medicines.value.orEmpty().toMutableMap()
        currentMap[medicine.barcode] = medicine
        _medicines.value = currentMap
    }

    fun createMedicine(medicine: Medicine, pharmacyID: String, quantity: Int) {
        val pharmacy = getPharmacyById(pharmacyID)
        pharmacy?.addNewMedicine(medicine.barcode, quantity)
    }


    fun addPharmacyIdToMedicine(medicine: Medicine, pharmacyId: String) {
        medicine.addPharmacyId(pharmacyId)
        // Update the medicine in the backend
        updateMedicineInBackend(medicine)
    }

    fun removePharmacyIdFromMedicine(medicine: Medicine, pharmacyId: String) {
        medicine.removePharmacyId(pharmacyId)
        // Update the medicine in the backend
        updateMedicineInBackend(medicine)
    }

    fun removeMedicineFromPharmacy(medicine: Medicine, pharmacyId: String) {
        val pharmacy = getPharmacyById(pharmacyId)
        if (pharmacy != null) {
            pharmacy.removeMedicine(medicine.barcode)
            // Update the pharmacy in the backend
            updatePharmacyInBackend(pharmacy)
        }
    }

    ////////////////////////////////PHARMACY FUNCTIONS///////////////////////////////////////////////
    private fun updatePharmacy(updatedPharmacy: Pharmacy) {
        val currentMap = _pharmacies.value.orEmpty().toMutableMap()
        currentMap[updatedPharmacy.id] = updatedPharmacy
        _pharmacies.value = currentMap
    }

    fun addPharmacyBackend(pharmacy: Pharmacy) {
        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            // Add the pharmacy to the backend
            try {
                // Add the pharmacy to the backend with ID
                Log.d("PharmaciesViewModel", "Adding pharmacy to the backend")
                db.collection("Pharmacies")
                    .document(pharmacy.id)
                    .set(pharmacy)
                    .addOnSuccessListener {
                        Log.d("PharmaciesViewModel", "Pharmacy added successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PharmaciesViewModel", "Error adding pharmacy", e)
                    }
            } catch (e: Exception) {
                // Handle the exception
                Log.e("PharmaciesViewModel", "Error occurred during adding pharmacy", e)
            }
        }
    }

    private fun fetchPharmacyData(pharmacyId: String) {
        viewModelScope.launch {
            val photoUrl = fetchPharmacyPhotosFromBackend(pharmacyId)
            if (photoUrl != null) {
                val pharmacy = getPharmacyById(pharmacyId)
                if (pharmacy != null) {
                    pharmacy.photo_path = photoUrl
                    updatePharmacy(pharmacy)
                }
            }
        }
    }

    suspend fun fetchPharmacyPhotosFromBackend(pharmacyId: String): String? {
        val storage = Firebase.storage
        val storageRef = storage.reference

        val photoRef =
            storageRef.child("data/user/0/com.ist.pharmacist/files/${pharmacyId}_photo.jpg")

        return try {
            val uri = photoRef.downloadUrl.await()
            uri.toString()
        } catch (exception: Exception) {
            Log.d("PharmaciesViewModel", "Pharmacy name: ${getPharmacyById(pharmacyId)?.name}")
            Log.e("PharmaciesViewModel", "Error fetching pharmacy photo", exception)
            null
        }
    }

    fun getAllPharmaciesFromBackend(context: Context) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            val location = getCurrentLocation(context)
            val (lowerLat, upperLat, lowerLng, upperLng) = calculateBounds(
                location!!,
                30.000000000000000
            )

            db.collection("Pharmacies")
                .whereGreaterThan("location.latitude", lowerLat)
                .whereLessThan("location.latitude", upperLat)
                .whereGreaterThan("location.longitude", lowerLng)
                .whereLessThan("location.longitude", upperLng)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("PharmaciesViewModel", "Error getting pharmacies", error)
                        return@addSnapshotListener
                    }
                    val pharmacyMap = mutableMapOf<String, Pharmacy>()
                    val medicineBarcodes = mutableSetOf<String>()
                    for (document in snapshots!!) {
                        Log.d("PharmaciesViewModel", "${document.id} => ${document.data}")
                        val pharmacyId = document.id
                        val pharmacyData = document.data
                        val pharmacy = extractPharmacyFromDocumentData(pharmacyId, pharmacyData)
                        pharmacyMap[pharmacyId] = pharmacy

                        // Collect the medicine barcodes from the pharmacy
                        medicineBarcodes.addAll(pharmacy.medicines.keys)
                        Log.d("PharmaciesViewModel", "Medicine Barcodes: $medicineBarcodes")

                        // Fetch the pharmacy photo only if not on a metered connection
                        if (!isMeteredConnection(context)) {
                            fetchPharmacyData(pharmacyId)
                        }
                    }
                    _pharmacies.value = pharmacyMap
                    _medicineBarcodes.value = medicineBarcodes.toList()
                }
        }
    }

    fun updatePharmacyInBackend(pharmacy: Pharmacy) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            // Create a map of the fields to update
            val updates = hashMapOf(
                "medicines" to pharmacy.medicines,
                "flaggers" to pharmacy.flaggers,
                "flaggedGlobally" to pharmacy.flaggedGlobally
                // Add other fields of the Pharmacy class that you want to update
            )

            try {
                // Update the pharmacy document in Firestore
                db.collection("Pharmacies")
                    .document(pharmacy.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d("PharmaciesViewModel", "Pharmacy updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PharmaciesViewModel", "Error updating pharmacy", e)
                    }
            } catch (e: Exception) {
                Log.e("PharmaciesViewModel", "Error occurred during updating pharmacy", e)
            }
        }
    }

    ////////////////////////////////MEDICINE FUNCTIONS///////////////////////////////////////////////
    fun addMedicineToBackend(medicine: Medicine, onSuccess: () -> Unit) {
        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            // Add the medicine to the backend
            try {
                // Add the medicine to the backend with barcode as the document ID
                Log.d("PharmaciesViewModel", "Adding medicine to the backend")
                db.collection("Medicines")
                    .document(medicine.barcode)
                    .set(medicine)
                    .addOnSuccessListener {
                        Log.d("PharmaciesViewModel", "Medicine added successfully")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PharmaciesViewModel", "Error adding medicine", e)
                    }
            } catch (e: Exception) {
                // Handle the exception
                Log.e("PharmaciesViewModel", "Error occurred during adding medicine", e)
            }
        }
    }

    private fun getAllMedicinesFromBackend(context: Context) {

        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            db.collection("Medicines")
                .whereIn("barcode", _medicineBarcodes.value.orEmpty())
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("PharmaciesViewModel", "Error getting medicines", error)
                        return@addSnapshotListener
                    }
                    val medicineMap = mutableMapOf<String, Medicine>()
                    for (document in snapshots!!) {
                        Log.d("PharmaciesViewModel", "${document.id} => ${document.data}")
                        val barcode = document.id
                        val medicineData = document.data
                        val medicine = extractMedicineFromDocumentData(barcode, medicineData)
                        medicineMap[barcode] = medicine

                        // Fetch the medicine photo only if not on a metered connection
                        if (!isMeteredConnection(context)) {
                            fetchMedicineData(barcode)
                        }
                    }
                    _medicines.value = medicineMap
                }
        }
    }



    suspend fun fetchMedicinePhotosFromBackend(medicineBarcode: String): String? {

        val storage = Firebase.storage
        val storageRef = storage.reference

        val photoRef =
            storageRef.child("data/user/0/com.ist.pharmacist/files/${medicineBarcode}_photo.jpg")

        return try {
            val uri = photoRef.downloadUrl.await()
            uri.toString()
        } catch (exception: Exception) {
            Log.d(
                "PharmaciesViewModel",
                "Medicine Name: ${getMedicineByBarcode(medicineBarcode)?.name}"
            )
            Log.e("PharmaciesViewModel", "Error fetching medicine photo", exception)
            null
        }
    }

    private fun fetchMedicineData(barcode: String) {
        viewModelScope.launch {
            val photoUrl = fetchMedicinePhotosFromBackend(barcode)
            if (photoUrl != null) {
                val medicine = getMedicineByBarcode(barcode)
                if (medicine != null) {
                    medicine.box_photo_path = photoUrl
                    addMedicine(medicine)
                }
            }
        }
    }

    fun verifyIfPharmacyIdExistsInTheMedicine(medicine: Medicine, pharmacyId: String): Boolean {
        return medicine.pharmacy_ids.contains(pharmacyId)
    }

    //////////////////////////////Getters///////////////////////////////////////////////

    fun getPharmacies(): MutableLiveData<Map<String, Pharmacy>> {
        return _pharmacies
    }

    fun getPharmacy(pharmacyId: String): Pharmacy? {
        return _pharmacies.value?.get(pharmacyId)
    }



    fun getPharmacyById(pharmacyId: String): Pharmacy? {
        return _pharmacies.value?.get(pharmacyId)
    }

    fun getMedicineByBarcode(medicineBarcode: String): Medicine? {
        return _medicines.value?.get(medicineBarcode)
    }

    fun getMedicines(): MutableLiveData<Map<String, Medicine>> {
        return _medicines
    }

    fun getAllMedicinesOfPharmacy(pharmacyId: String): Map<String, Medicine> {
        val pharmacy = getPharmacyById(pharmacyId)
        val medicines = mutableMapOf<String, Medicine>()
        pharmacy?.medicines?.forEach { (barcode, _) ->
            val medicine = getMedicineByBarcode(barcode)
            if (medicine != null) {
                medicines[barcode] = medicine
            }
        }
        return medicines
    }

    suspend fun getUserByUsername(username: String?): User? {
        return if (username != null) {
            val db = FirebaseFirestore.getInstance()
            var user: User? = null
            try {
                val querySnapshot = db.collection("Profiles")
                    .whereEqualTo("username", username)
                    .get()
                    .await()
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    user = documentSnapshot.data?.let { extractUserFromDocumentData(it) }
                    Log.d("PharmaciesViewModel", "User: $user")
                }
            } catch (exception: Exception) {
                Log.e("PharmaciesViewModel", "Error getting user", exception)
            }
            user
        } else {
            null
        }
    }
///////////////////////////////////////////////////////////////////////////////////////////////////
    fun verifyIfBarcodeExists(barcode: String): Boolean {
        return _medicines.value?.containsKey(barcode) ?: false
    }

    //Pop up menu handling

    fun activateAddMedicine() {
        _addMedicine.value = true
    }

    fun deactivateAddMedicine() {
        _addMedicine.value = false
    }

    fun activateUseMedicine() {
        _removeMedicine.value = true
    }

    fun deactivateUseMedicine() {
        _removeMedicine.value = false
    }


    fun activateFlagPharmacyMenu() {
        _flagPharmacyMenu.value = true
    }

    fun deactivateFlagPharmacyMenu() {
        _flagPharmacyMenu.value = false
    }


    ///////////////////////////////////////Searchers//////////////////////////////////////////////////////////////

    @SuppressLint("DefaultLocale")
    suspend fun searchPharmaciesWithMedicineName(
        medicineName: String,
        context: Context
    ): List<String> {
        Log.d("SearchPharmacies", "Searching pharmacies with medicine: $medicineName")
        Log.d("Pharmacies", _pharmacies.value.toString())
        Log.d("Medicines", _medicines.value.toString())

        return withContext(Dispatchers.IO) {
            val pharmaciesWithMedicine = mutableListOf<String>()

            val currentLocation = getCurrentLocation(context)
            Log.d("CurrentLocation", "Current location: $currentLocation")

            if (currentLocation != null) {
                val pharmaciesWithDistances = mutableListOf<Pair<Pharmacy, Double>>()

                for (pharmacy in _pharmacies.value?.values ?: emptyList()) {
                    for (medicine in pharmacy.medicines.keys) {
                        val medicineDetails = getMedicineByBarcode(medicine)
                        Log.d("MedicineDetails", "Medicine: $medicine, Details: $medicineDetails")
                        if (medicineDetails?.name?.contains(
                                medicineName,
                                ignoreCase = true
                            ) == true
                        ) {
                            Log.d(
                                "MedicineDetails",
                                "Found matching medicine: ${medicineDetails.name}"
                            )
                            val distance =
                                calculateDistance(currentLocation, pharmacy.location) / 1000
                            pharmaciesWithDistances.add(Pair(pharmacy, distance))
                            break
                        }
                    }
                }

                Log.d("PharmaciesWithDistances", pharmaciesWithDistances.toString())

                val sortedPharmacies = pharmaciesWithDistances.sortedBy { it.second }
                Log.d("SortedPharmacies", sortedPharmacies.toString())

                val formattedPharmacies = sortedPharmacies.map { pair ->
                    "${pair.first.id} - ${String.format("%.2f", pair.second)} km"
                }
                Log.d("FormattedPharmacies", formattedPharmacies.toString())

                pharmaciesWithMedicine.addAll(formattedPharmacies)
                Log.d("PharmaciesWithMedicine", pharmaciesWithMedicine.toString())
            }

            pharmaciesWithMedicine
        }
    }

    @SuppressLint("DefaultLocale")
    suspend fun searchPharmaciesWithMedicine(medicine: Medicine, context: Context): List<String> {
        Log.d("SearchPharmacies", "Searching pharmacies with medicine: ${medicine.name}")

        return withContext(Dispatchers.IO) {
            val pharmaciesWithMedicine = mutableListOf<String>()

            val currentLocation = getCurrentLocation(context)
            Log.d("CurrentLocation", "Current location: $currentLocation")

            if (currentLocation != null) {
                val pharmaciesWithDistances = mutableListOf<Pair<Pharmacy, Double>>()

                for (pharmacyId in medicine.pharmacy_ids) {
                    val pharmacy = getPharmacyById(pharmacyId)
                    if (pharmacy != null) {
                        val distance = calculateDistance(currentLocation, pharmacy.location) / 1000
                        pharmaciesWithDistances.add(Pair(pharmacy, distance))
                    }
                }

                Log.d("PharmaciesWithDistances", pharmaciesWithDistances.toString())

                val sortedPharmacies = pharmaciesWithDistances.sortedBy { it.second }
                Log.d("SortedPharmacies", sortedPharmacies.toString())

                val formattedPharmacies = sortedPharmacies.map { pair ->
                    "${pair.first.id} - ${String.format("%.2f", pair.second)} km"
                }
                Log.d("FormattedPharmacies", formattedPharmacies.toString())

                pharmaciesWithMedicine.addAll(formattedPharmacies)
                Log.d("PharmaciesWithMedicine", pharmaciesWithMedicine.toString())
            }

            pharmaciesWithMedicine
        }
    }

    fun searchPharmacies(query: String, pharmacies: Map<String, Pharmacy>): List<Pharmacy> {
        return pharmacies.values.filter { pharmacy ->
            pharmacy.name.contains(query, ignoreCase = true)
        }
    }


    ///////////////////////////////////////Favorite and Notifications///////////////////////////////

    private fun addFavoritePharmacy(user: User, pharmacyId: String) {
        user.addFavoritePharmacy(pharmacyId)
        // Update the user's favorite pharmacies in the backend
        updateUserInBackend(user)
    }

    private fun removeFavoritePharmacy(user: User, pharmacyId: String) {
        user.removeFavoritePharmacy(pharmacyId)
        // Update the user's favorite pharmacies in the backend
        updateUserInBackend(user)
    }

    private fun addUserToNotify(medicine: Medicine, username: String) {
        medicine.addUserToNotify(username)
        // Update the medicine in the backend
        updateMedicineInBackend(medicine)
    }

    private fun removeUserToNotify(medicine: Medicine, username: String) {
        medicine.removeUserToNotify(username)
        // Update the medicine in the backend
        updateMedicineInBackend(medicine)
    }

    fun getFavouritePharmaciesFromViewModel(user: User): List<Pharmacy> {
        val favouritePharmacies = mutableListOf<Pharmacy>()
        for (pharmacyId in user.favoritePharmacies) {
            val pharmacy = getPharmacyById(pharmacyId)
            if (pharmacy != null) {
                favouritePharmacies.add(pharmacy)
            }
        }
        return favouritePharmacies
    }





    //////////////////////////////////Meta Moderation and Favorite Pharmacy Handle//////////////////

    private fun addFlaggedPharmacy(user: User, pharmacyId: String) {
        // Add the pharmacy to the user's flagged pharmacies
        user.addFlaggedPharmacy(pharmacyId)

        //Add the user to the list of flaggers of the pharmacy
        val pharmacy = getPharmacyById(pharmacyId)
        pharmacy?.addFlagger(user.username)

        flagGloballyPharmacy(pharmacyId)

        // Update the user's flagged pharmacies in the backend
        updateUserInBackend(user)
        // Update the pharmacy in the backend
        updatePharmacyInBackend(pharmacy!!)
    }

    private fun flagGloballyPharmacy(pharmacyId: String) {

        viewModelScope.launch {
            val pharmacy = getPharmacyById(pharmacyId)

            if (pharmacy != null && pharmacy.flaggers.size >= 3) {
                pharmacy.flagPharmacy()
                // Update the pharmacy in the backend
                updatePharmacyInBackend(pharmacy)
            }
        }
    }

    fun isUserSuspended(user: User): Boolean {
        return user.suspended
    }

    fun toggleUserToNotify(medicine: Medicine, username: String) {
        if (medicine.isUserToNotify(username)) {
            removeUserToNotify(medicine, username)
        } else {
            addUserToNotify(medicine, username)
        }
    }
    fun toggleFavoritePharmacy(user: User, pharmacyId: String) {
        if (user.isFavoritePharmacy(pharmacyId)) {
            removeFavoritePharmacy(user, pharmacyId)
        } else {
            addFavoritePharmacy(user, pharmacyId)
        }
    }


    fun suspendUserIfNeeded(user: User) {
        viewModelScope.launch {
            val pharmaciesCreatedByUser = user.pharmaciesCreatedByUser
            var counter = 0

            _pharmacies.observeForever { pharmacies ->
                for (pharmacyId in pharmaciesCreatedByUser) {
                    val pharmacy = pharmacies[pharmacyId]
                    if (pharmacy != null && pharmacy.isFlagged()) {
                        counter++
                    }
                }

                if (counter >= 3) {
                    user.suspendUser()
                    updateUserInBackend(user)
                }

            }

            _pharmacies.removeObserver {}
        }

    }

    fun toggleFlaggedPharmacy(user: User, pharmacyId: String) {
        addFlaggedPharmacy(user, pharmacyId)
    }

    fun removePharmaciesFlaggedFromMap(user: User) {
        viewModelScope.launch {
            val pharmaciesFlagged = user.getFlaggedPharmacies()
            val currentMap = _pharmacies.value.orEmpty().toMutableMap()
            for (pharmacyId in pharmaciesFlagged) {
                currentMap.remove(pharmacyId)
            }
            _pharmacies.value = currentMap
        }
    }

   fun removePharmaciesFlaggedGloballyFromMap() {
       viewModelScope.launch {
           val currentMap = _pharmacies.value.orEmpty().toMutableMap()
           val pharmaciesToRemove = mutableListOf<String>()
           for (pharmacy in currentMap.values) {
               if (pharmacy.isFlagged()) {
                   pharmaciesToRemove.add(pharmacy.id)
               }
           }
           pharmaciesToRemove.forEach { currentMap.remove(it) }
           _pharmacies.value = currentMap
       }

    }

    //////////////////////////////Update Backend////////////////////////////////////////////////////

    private fun updateMedicineInBackend(medicine: Medicine) {

        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            val updates = hashMapOf<String, Any>(
                "users_to_notify" to medicine.users_to_notify,
                "pharmacy_ids" to medicine.pharmacy_ids
                // Add other fields of the Medicine class that you want to update
            )

            try {
                // Update the medicine document in Firestore
                db.collection("Medicines")
                    .document(medicine.barcode)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d("PharmaciesViewModel", "Medicine updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PharmaciesViewModel", "Error updating medicine", e)
                    }
            } catch (e: Exception) {
                Log.e("PharmaciesViewModel", "Error occurred during medicine update", e)
            }
        }
    }

    private fun updateUserInBackend(user: User) {
        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            try {
                // Update the user document in Firestore
                db.collection("Profiles")
                    .document(user.username)
                    .set(user)
                    .addOnSuccessListener {
                        Log.d("PharmaciesViewModel", "User updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PharmaciesViewModel", "Error updating user", e)
                    }
            } catch (e: Exception) {
                Log.e("PharmaciesViewModel", "Error occurred during user update", e)
            }
        }
    }

    ////////////////////////////////Update Repositories And View Model//////////////////////////////

    //Update the local repositories with the latest data from the backend
    fun updateRepositories() {
        Log.d("PharmaciesViewModel", "Updating Repositories..")

        viewModelScope.launch {
            val localPharmacies = pharmacyRepository.getAllPharmacies()
            val localMedicines = medicineRepository.getAllMedicines()

            val pharmaciesToUpdate = mutableListOf<PharmacyCacheData>()
            val medicinesToUpdate = mutableListOf<MedicineCacheData>()

            // Check for changes in pharmacies
            for (pharmacy in _pharmacies.value.orEmpty().values) {
                val pharmacyCacheData = pharmacy.toPharmacyCacheData()
                val localPharmacy = localPharmacies.find { it.id == pharmacyCacheData.id }
                Log.d(
                    "PharmaciesViewModel",
                    "Local Pharmacy: $localPharmacy, Pharmacy: $pharmacyCacheData"
                )
                if (localPharmacy == null || pharmacyCacheData != localPharmacy) {
                    pharmaciesToUpdate.add(pharmacyCacheData)
                }
            }

            // Check for changes in medicines
            for (medicine in _medicines.value.orEmpty().values) {
                val medicineCacheData = medicine.toMedicineCacheData()
                val localMedicine = localMedicines.find { it.barcode == medicineCacheData.barcode }
                Log.d(
                    "PharmaciesViewModel",
                    "Local Medicine: $localMedicine, Medicine: $medicineCacheData"
                )
                if (localMedicine == null || medicineCacheData != localMedicine) {
                    medicinesToUpdate.add(medicineCacheData)
                }
            }

            // Update the local repositories only if there are changes
            if (pharmaciesToUpdate.isNotEmpty()) {
                pharmaciesToUpdate.forEach {
                    pharmacyRepository.insertPharmacy(it)
                }
                Log.d(
                    "PharmaciesViewModel",
                    "Updated ${pharmaciesToUpdate.size} pharmacies in local repository"
                )
            } else {
                Log.d("PharmaciesViewModel", "No changes in pharmacies")
            }

            if (medicinesToUpdate.isNotEmpty()) {
                medicinesToUpdate.forEach {
                    medicineRepository.insertMedicine(it)
                }
                Log.d(
                    "PharmaciesViewModel",
                    "Updated ${medicinesToUpdate.size} medicines in local repository"
                )
            } else {
                Log.d("PharmaciesViewModel", "No changes in medicines")
            }
        }
    }

    // Update the view model with the latest data from the local repositories
    private fun updateViewModelWithRepositories() {
        viewModelScope.launch {
            //Update the local pharmacy repository
            val pharmacies = pharmacyRepository.getAllPharmacies()
            val pharmacyMap = mutableMapOf<String, Pharmacy>()
            for (pharmacy in pharmacies) {
                val pharmacyObject = Pharmacy(
                    id = pharmacy.id,
                    ownerName = pharmacy.ownerName,
                    name = pharmacy.name,
                    address = pharmacy.address,
                    location = pharmacy.location,
                    photo_path = pharmacy.photo_path,
                    medicines = pharmacy.medicines,
                    flaggers = pharmacy.flaggers,
                    flaggedGlobally = pharmacy.flaggedGlobally

                )
                pharmacyMap[pharmacy.id] = pharmacyObject
            }
            _pharmacies.value = pharmacyMap

            //Update the local medicine repository
            val medicines = medicineRepository.getAllMedicines()
            val medicineMap = mutableMapOf<String, Medicine>()
            for (medicine in medicines) {
                val medicineObject = Medicine(
                    name = medicine.name,
                    box_photo_path = medicine.box_photo_path,
                    barcode = medicine.barcode,
                    purpose_preference = medicine.purpose_preference,
                    users_to_notify = medicine.users_to_notify,
                    pharmacy_ids = medicine.pharmacy_ids
                )
                medicineMap[medicine.barcode] = medicineObject
            }
            _medicines.value = medicineMap
        }
    }


    fun updateViewModel(context: Context, user: User) {
        viewModelScope.launch {
            Log.d("PharmaciesViewModel", "Updating ViewModel..")
            //Get all pharmacies from backend
            getAllPharmaciesFromBackend(context)

            // Observe changes in medicineBarcodes
            _medicineBarcodes.observeForever { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Call getAllMedicinesFromBackend only when medicineBarcodes is not empty
                    getAllMedicinesFromBackend(context)
                    Log.d("PharmaciesViewModel", "Medicines: ${_medicines.value}")

                    updateRepositories()

                    if (getPharmacies().value.isNullOrEmpty() && getMedicines().value.isNullOrEmpty()) {
                        updateViewModelWithRepositories()
                    }

                    // Remove the observer to avoid multiple invocations
                    _medicineBarcodes.removeObserver {}
                }
            }
        }

    }


}