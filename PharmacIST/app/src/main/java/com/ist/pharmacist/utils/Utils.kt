package com.ist.pharmacist.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.IOException
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.ist.pharmacist.R
import com.ist.pharmacist.data.Medicine
import com.ist.pharmacist.data.Pharmacy
import com.ist.pharmacist.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos


fun checkForPermission(context: Context): Boolean {
    return !(ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED)
}


@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): LatLng? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }

        val locationResult = fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null).await()
        if (locationResult != null) {
            Log.d("MainScreen", "Current location: ${locationResult.latitude}, ${locationResult.longitude}")
            return LatLng(locationResult.latitude, locationResult.longitude)
        } else {
            Log.d("MainScreen", "Current location is null")
        }
    } catch (e: Exception) {
        Log.e("MainScreen", "Error getting current location", e)
    }
    return null
}

// Fetch place details using the Places SDK to get the latitude and longitude of a place
suspend fun fetchPlaceDetails(context: Context, placeId: String, onSuccess: (LatLng) -> Unit) {
    val placesClient = Places.createClient(context)
    val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG)).build()

    try {
        val response = placesClient.fetchPlace(request).await()
        val place = response.place
        val latLng = place.latLng
        if (latLng != null) {
            onSuccess(latLng)
        } else {
            Log.e("PlaceDetails", "Latitude/Longitude not found for place: $placeId")
        }
    } catch (e: Exception) {
        Log.e("PlaceDetails", "Error fetching place details for placeId: $placeId", e)
    }
}



// save image to internal storage
suspend fun saveImageToInternalStorage(context: Context, imageUri: Uri, pharmacyName: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val outputFile = File(context.filesDir, "${pharmacyName}_photo.jpg")

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input?.copyTo(output)
                }
            }

            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}


//get address from location
fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
    if (addresses != null) {
       if (addresses.isNotEmpty()) {
            return addresses[0].getAddressLine(0) // This will give you the complete address.
        } else {
           return "Address not found"
        }
    }
    return "Address not found"
}


//Get the pharmacy from the document data that was fetched from Firestore
fun extractPharmacyFromDocumentData(pharmacyID: String, documentData: Map<String, Any>): Pharmacy {

    val ownerName = documentData["ownerName"] as String
    val name = documentData["name"] as String
    val address = documentData["address"] as String
    val photoPath = documentData["photo_path"] as String

    // Extract the location map
    val locationMap = documentData["location"] as Map<*, *>
    val latitude = locationMap["latitude"] as Double
    val longitude = locationMap["longitude"] as Double
    val location = LatLng(latitude, longitude)

    val pharmacy = Pharmacy(pharmacyID, ownerName, name, address, location, photoPath)


    // Extract the medicines map
    val medicinesArray = documentData["medicines"] as HashMap<String, Int>

    medicinesArray.forEach { (barcode, quantity) ->
        pharmacy.addNewMedicine(barcode, quantity)
    }

    //Extract the flaggers list
    val flaggersList = documentData["flaggers"] as List<String>? ?: emptyList()

    flaggersList.forEach { flagger ->
        pharmacy.addFlagger(flagger)
    }


    //Extract the flagged boolean
    val flagged = documentData["flaggedGlobally"] as Boolean

    pharmacy.setflagPharmacy(flagged)


    // Create and return a Pharmacy object
    return pharmacy
}


//Extract the medicine from the document data that was fetched from Firestore
fun extractMedicineFromDocumentData(medicineBarcode: String, documentData: Map<String, Any>): Medicine {
    val name = documentData["name"] as String
    val boxPhotoPath = documentData["box_photo_path"] as String
    val purposePreference = documentData["purpose_preference"] as String

    // Extract the users_to_notify list
    val usersToNotifyList = documentData["users_to_notify"] as List<String>

    // Extract the pharmacy_ids list
    val pharmacyIdsList = documentData["pharmacy_ids"] as List<String>

    // Create and return a Medicine object
    return Medicine(name, boxPhotoPath, medicineBarcode, purposePreference, usersToNotifyList.toMutableList(), pharmacyIdsList.toMutableList())
}

//Extract the user from the document data that was fetched from Firestore
fun extractUserFromDocumentData(documentData: Map<String, Any>): User {
    val username = documentData["username"] as String
    val email = documentData["email"] as String
    val password = documentData["password"] as String

    // Extract the pharmaciesCreatedByUser list
    val pharmaciesCreatedByUserList = documentData["pharmaciesCreatedByUser"] as List<String>? ?: emptyList()

    // Extract the favoritePharmacies list
    val favoritePharmaciesList = documentData["favoritePharmacies"] as List<String>? ?: emptyList()

    //Extract the pharmaciesFlagged list
    val pharmaciesFlaggedList = documentData["pharmaciesFlagged"] as List<String>? ?: emptyList()

    // Extract the suspended boolean
    val suspended = documentData["suspended"] as Boolean


    // Create and return a User object
    return User(username, email, password,pharmaciesCreatedByUserList.toMutableList(), favoritePharmaciesList.toMutableList(), pharmaciesFlaggedList.toMutableList(), suspended)
}




fun getLatLngFromAddress(context: Context, mAddress: String): LatLng? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: MutableList<Address>? = geocoder.getFromLocationName(mAddress, 1)
    if (addresses != null) {
        if (addresses.isNotEmpty()) {
            val latitude = addresses.get(0).latitude
            val longitude = addresses.get(0).longitude
            return LatLng(latitude, longitude)
        }
    }
    return null
}

//Calculate the bounds of the region around the user
fun calculateBounds(location: LatLng, radius: Double) : Array<Double> {
    val earthRadius = 6371.0
    val latRadius = Math.toDegrees(radius / earthRadius)
    val lngRadius = Math.toDegrees(radius / (earthRadius * cos(Math.toRadians(location.latitude))))

    val lowerLat = location.latitude - latRadius
    val upperLat = location.latitude + latRadius
    val lowerLng = location.longitude - lngRadius
    val upperLng = location.longitude + lngRadius

    return arrayOf(lowerLat, upperLat, lowerLng, upperLng)
}

//Calculate the distance between two locations
fun calculateDistance(location1: LatLng, location2: LatLng): Double {
    val results = FloatArray(1)
    Location.distanceBetween(
        location1.latitude,
        location1.longitude,
        location2.latitude,
        location2.longitude,
        results
    )
    return results[0].toDouble()
}

//Send notification for nearby pharmacies in this case 100 meters close to the user
@SuppressLint("UnspecifiedImmutableFlag")
private fun sendNotificationPharmacy(context: Context, pharmacyCount: Int) {
    val notificationManager = NotificationManagerCompat.from(context)

    val channelId = "nearby_pharmacies_channel"
    val channelName = "Nearby Pharmacies"
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }

    val notificationId = 1
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.login_image)
        .setContentTitle("Nearby Pharmacies")
        .setContentText("$pharmacyCount pharmacies are in your region")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, builder.build())
        }
    } else {
        notificationManager.notify(notificationId, builder.build())
    }
}

fun checkNearbyPharmaciesAndNotify(
    context: Context,
    currentLocation: LatLng,
    pharmacies: MutableLiveData<Map<String, Pharmacy>>
) {
    val nearbyPharmacies = pharmacies.value?.values?.filter { pharmacy ->
        Log.d("Distance", "Distance: ${calculateDistance(currentLocation, pharmacy.location)}")
        calculateDistance(currentLocation, pharmacy.location) <= 100
    }
    Log.d("NearbyPharmacies", "Nearby pharmacies: $nearbyPharmacies")

    nearbyPharmacies?.let {
        if (it.isNotEmpty()) {
            sendNotificationPharmacy(context, it.size)
        }
    }
}


//Send notification for medicine when it is available in their favourite pharmacies
@SuppressLint("UnspecifiedImmutableFlag")
private fun sendNotificationMedicine(context: Context, medicineName: String, pharmacyName: String, medicineQuantity: Int,  notificationId: Int) {
    val notificationManager = NotificationManagerCompat.from(context)

    val channelId = "medicine_notification_channel"
    val channelName = "Medicine Notifications"
    val importance = NotificationManager.IMPORTANCE_DEFAULT

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
    }

    val notificationId = notificationId
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.login_image)
        .setContentTitle("Medicine Notification")
        .setContentText("$medicineName is available in your favorite pharmacy: $pharmacyName with stock: $medicineQuantity" )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, builder.build())
        }
    } else {
        notificationManager.notify(notificationId, builder.build())
    }
}

//Check if the medicine is available in the user's favorite pharmacies and notify him
fun checkMedicineAvailabilityAndNotify(
    context: Context,
    medicine: Medicine,
    pharmacies: MutableLiveData<Map<String, Pharmacy>>,
    user: User,
    notificationId: Int
) : Int{
    val favoritePharmacies = pharmacies.value?.values?.filter { pharmacy ->
        user.favoritePharmacies.contains(pharmacy.id)
    }
    var notificationId = notificationId
    favoritePharmacies?.let { pharmacies ->
        for (pharmacy in pharmacies) {
            if (pharmacy.medicines.containsKey(medicine.barcode)) {
                notificationId += 1
                sendNotificationMedicine(context, medicine.name, pharmacy.name, pharmacy.getMedicineQuantity(medicine.barcode)!! , notificationId)
            }
        }
    }
    return notificationId
}

//Notify the user with the medicines that are available in their favorite pharmacies and he asked to be notified about them
fun notifyUserWithMedicines(
    context: Context,
    user: User,
    medicines : MutableLiveData<Map<String, Medicine>>,
    pharmacies: MutableLiveData<Map<String, Pharmacy>>
){
    val medicinesToNotify = medicines.value?.values?.filter { medicine ->
        user.username in medicine.users_to_notify
    }
    var notificationIdFirst = 1
    var notificationIdSecond = 1
    medicinesToNotify?.let {
        for (medicine in medicinesToNotify) {
            notificationIdSecond = checkMedicineAvailabilityAndNotify(context, medicine, pharmacies, user, notificationIdFirst)
            notificationIdFirst = notificationIdSecond
        }
    }
}


//Draw a track to the pharmacy using Google Maps to navigate the user
fun drawTrack(pharmacyLatitude: Double, pharmacyLongitude: Double, context: Context) {
    try {
        // Create a URI with the pharmacy's latitude and longitude
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$pharmacyLatitude,$pharmacyLongitude")

        // Initialize an intent with action view
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Start the activity
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // If Google Maps is not installed, redirect the user to the Google Play Store
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

//Check if the user is connected to a metered connection
fun isMeteredConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
}

fun isFieldValid(value: String): Boolean {
    return value.isNotBlank()
}

fun isImageValid(imagePath: String?): Boolean {
    return imagePath != null
}



