package com.ist.pharmacist.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ist.pharmacist.PharmacyPreferences
import com.ist.pharmacist.R
import com.ist.pharmacist.data.Medicine
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.theme.MapStyle
import com.ist.pharmacist.ui.views.NetworkStateViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.ui.views.PhotoCacheViewModel
import com.ist.pharmacist.utils.BarcodeScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyPanelScreen(
    context: Context,
    backHandler: () -> Unit,
    pharmaciesViewModel: PharmaciesViewModel,
    photoCacheViewModel: PhotoCacheViewModel,
    networkStateViewModel: NetworkStateViewModel,
    pharmacyID: String,
    onCreateMedicineClick: (String) -> Unit = {},
    onGoThereClick: (LatLng?) -> Unit,
    onMedicineClick: (String) -> Unit,
    user: User,
    onFavoriteClick: (String) -> Unit,
    onFlagClick: (String) -> Unit
) {

    val pharmacies by pharmaciesViewModel.getPharmacies().observeAsState(emptyMap())

    val pharmacy = pharmacies[pharmacyID]

    val isGuestUser = (user.username == "Guest")

    var medicines = pharmaciesViewModel.getAllMedicinesOfPharmacy(pharmacyID)

    val pharmacyPreferences = PharmacyPreferences(context)
    val coroutineScope = rememberCoroutineScope()

    val inriaSerifFont = FontFamily(
        Font(R.font.inria_serif_regular),
    )

    var isFavorite by remember { mutableStateOf(user.isFavoritePharmacy(pharmacyID)) }

    val medicinesAdd = createPopUpMenuToAddQuantity(context, pharmacyID, pharmaciesViewModel)

    val medicinesReduce = createPopUpMenuToRemoveQuantity(context, pharmacyID, pharmaciesViewModel)

    CreatePopMenuToConfirmFlag(pharmacyID, pharmaciesViewModel, onFlagClick)

    // Photo Handling
    val isMeteredConnection by networkStateViewModel.isMeteredConnection.collectAsState()
    var pharmacyPhotoUrl by remember { mutableStateOf<String?>(null) }
    var hasLocalPhoto by remember { mutableStateOf(false) }
    var localPhotoPath by remember { mutableStateOf<String?>(null) }

    val pharmacyPhotoCache = photoCacheViewModel.pharmacyPhotoCache

    LaunchedEffect(pharmacyID) {
        coroutineScope.launch {
            Log.d("PharmacyPanelScreen", "Checking for local pharmacy photo")
            hasLocalPhoto = withContext(Dispatchers.IO) {
                pharmacyPreferences.hasPharmacyPhoto(pharmacyID)
            }
            localPhotoPath = withContext(Dispatchers.IO) {
                pharmacyPreferences.getPharmacyPhotoPath(pharmacyID)
            }
        }
    }

    // Observe network state changes
    LaunchedEffect(isMeteredConnection) {
        if (!isMeteredConnection && pharmacyPhotoUrl == null && !hasLocalPhoto) {
            // Load photo from cache or fetch from backend
            if (pharmacyPhotoCache.containsKey(pharmacyID)) {
                pharmacyPhotoUrl = pharmacyPhotoCache[pharmacyID]
            } else if (pharmacy != null) {
                // Fetch the pharmacy photo from the backend
                val photoUrl = pharmaciesViewModel.fetchPharmacyPhotosFromBackend(pharmacyID)
                if (photoUrl != null) {
                    pharmacyPhotoUrl = photoUrl
                    photoCacheViewModel.cachePharmacyPhoto(pharmacyID, pharmacyPhotoUrl!!)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(id = R.string.pharmacy_information_panel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        /*Back to previous screen*/
                        backHandler()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            tint = colorResource(id = R.color.black),
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                verticalArrangement = Arrangement.Top
            ) {
                if (pharmacy != null) {
                    Log.d("PharmacyPanelScreen", "Pharmacy found: ${pharmacy.name}")

                    // Optimize image loading by using Coil or Glide library
                    val imageModifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)


                    Box(modifier = imageModifier) {
                        if (hasLocalPhoto && localPhotoPath != null) {
                            AsyncImage(
                                model = localPhotoPath?.let { File(it) },
                                contentDescription = "Pharmacy Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (pharmacyPhotoUrl != null || pharmacyPhotoCache.containsKey(pharmacyID)) {
                            AsyncImage(
                                model = pharmacyPhotoUrl ?: pharmacyPhotoCache[pharmacyID],
                                contentDescription = "Pharmacy Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        if (networkStateViewModel.isOnline(context)) {
                                            if (isMeteredConnection) {
                                                coroutineScope.launch {
                                                    val photoUrl =
                                                        pharmaciesViewModel.fetchPharmacyPhotosFromBackend(
                                                            pharmacyID
                                                        )
                                                    if (photoUrl != null) {
                                                        pharmacyPhotoUrl = photoUrl
                                                        photoCacheViewModel.cachePharmacyPhoto(
                                                            pharmacyID,
                                                            photoUrl
                                                        )
                                                    }
                                                }
                                            } else {
                                                pharmacyPhotoUrl = pharmacy.photo_path
                                                photoCacheViewModel.cachePharmacyPhoto(
                                                    pharmacyID,
                                                    pharmacyPhotoUrl!!
                                                )
                                            }
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "No internet connection",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Tap to load photo",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        // Add the share button as an icon button on the top left corner
                        IconButton(
                            onClick = {
                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "Check out this pharmacy: ${pharmacy.name} at ${pharmacy.address} on PharmacIST!"
                                    )
                                    type = "text/plain"
                                }

                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 1f), shape = CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share Icon",
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center),
                                tint = Color.Black
                            )
                        }
                    }



                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pharmacy.name,
                            fontSize = 26.sp,
                            fontFamily = inriaSerifFont,
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.Center
                        )

                        // ADD REST OF UI HERE
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(285.dp),
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(200.dp, 200.dp)
                                    .padding(start = 20.dp, bottom = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                GoogleMap(
                                    modifier = Modifier.matchParentSize(),
                                    cameraPositionState = rememberCameraPositionState {
                                        position =
                                            CameraPosition.fromLatLngZoom(pharmacy.location, 15f)
                                    },
                                    properties = MapProperties(
                                        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                                            context,
                                            MapStyle.mapStyle
                                        )
                                    ),
                                    uiSettings = MapUiSettings(
                                        zoomControlsEnabled = false,
                                        zoomGesturesEnabled = false,
                                        myLocationButtonEnabled = false,
                                        scrollGesturesEnabled = false,
                                        scrollGesturesEnabledDuringRotateOrZoom = false,
                                        compassEnabled = false
                                    )
                                ) {
                                    Marker(
                                        state = MarkerState(position = pharmacy.location),
                                        title = pharmacy.name,
                                        snippet = pharmacy.address,
                                        icon = bitmapDescriptorWithSizeAndColor(
                                            context,
                                            R.drawable.pharmacy_marker,
                                            100,
                                            100,
                                            ContextCompat.getColor(
                                                context,
                                                if (isFavorite) com.google.android.libraries.places.R.color.quantum_yellow300 else R.color.green
                                            )
                                        )
                                    )
                                }
                            }

                            Button(
                                modifier = Modifier
                                    .padding(start = 60.dp, bottom = 10.dp),
                                // grey button
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                onClick = {
                                    // Fetch place details and animate there

                                    Log.d("PharmacyPanelScreen", "PlaceID: ${pharmacy.id}")
                                    coroutineScope.launch {
                                        onGoThereClick(pharmacy.location)
                                    }
                                }
                            ) {
                                Text(text = "Go There", color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }


                        }


                        val state = rememberLazyListState()

                        Log.d(
                            "PharmacyPanelScreen",
                            "Medicines: ${pharmacy.medicines.values.toList()}"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .width(164.dp)
                                    .height(400.dp)
                                    .padding(end = 12.dp)
                            ) {
                                LazyColumn(
                                    state = state,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                        .scrollbar(state, horizontal = false)

                                ) {
                                    Log.d("PharmacyPanelScreen", "Medicines: ${medicines.toList()}")
                                    items(medicines.toList()) { medicine ->
                                        Button(
                                            onClick = {
                                                //Go to medicine details screen
                                                onMedicineClick(medicine.first)
                                            },
                                            contentPadding = PaddingValues(8.dp),
                                            modifier = Modifier
                                                .width(155.dp)
                                                .height(55.dp)
                                                .padding(vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            ),
                                            shape = RoundedCornerShape(8.dp), // Set the button shape

                                        ) {
                                            Row(
                                                modifier = Modifier.padding(5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.pill_light_icon),
                                                    contentDescription = "Medicine Icon",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Black
                                                )
                                                Text(
                                                    text = medicine.second.name,
                                                    fontSize = 10.sp,
                                                    fontFamily = inriaSerifFont,
                                                    color = Color.Black,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.size(60.dp)
                                                )
                                                Text(
                                                    text = "Qty. ${
                                                        pharmacy.getMedicineQuantity(
                                                            medicine.first
                                                        )
                                                    }",
                                                    fontSize = 8.sp,
                                                    fontFamily = inriaSerifFont,
                                                    color = Color.Gray,
                                                    modifier = Modifier.size(55.dp)

                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {

                        Column {
                            Button(
                                onClick = { // Add to favorites
                                    if (!isGuestUser) {
                                        isFavorite = !isFavorite
                                        onFavoriteClick(pharmacyID)
                                    }
                                    else {
                                        Toast.makeText(
                                            context,
                                            "Guest users cannot add pharmacies to favorites",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .width(200.dp),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = com.google.android.libraries.places.R.color.quantum_yellow300),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isFavorite) R.drawable.fav_filled else R.drawable.fav_outlined
                                    ),
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(if (isFavorite) "Remove from favorites" else "Add to favorites")
                            }

                            Button(
                                onClick = { // Flag pharmacy

                                    //If the user created the pharmacy, they cannot flag it
                                    if (isGuestUser){
                                        Toast.makeText(
                                            context,
                                            "Guest users cannot flag pharmacies",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else if (pharmacy.ownerName == user.username) {
                                        Toast.makeText(
                                            context,
                                            "You cannot flag your own pharmacy",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else {
                                        pharmaciesViewModel.activateFlagPharmacyMenu()
                                    }
                                },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .width(200.dp),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = 8.dp
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = Color.Black
                                )
                            ){

                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = "Flag Icon",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )

                                Text(
                                    text = stringResource(id = R.string.flag_pharmacy),
                                )
                            }


                        }



                        Box(
                            modifier = Modifier
                                .width(175.dp)
                                .height(125.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Finalize the medicine addition
                                    if (networkStateViewModel.isOnline(context) && !isGuestUser) {
                                        onCreateMedicineClick(pharmacyID)
                                    }
                                    else if (isGuestUser) {
                                        Toast.makeText(
                                            context,
                                            "Guest users cannot add medicines",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else {
                                        Toast.makeText(
                                            context,
                                            "No internet connection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .padding(start = 20.dp, end = 10.dp, bottom = 10.dp)
                                    .align(Alignment.TopEnd)
                                    .width(60.dp)
                                    .height(60.dp)
                                    .align(Alignment.TopEnd)
                            )
                            {
                                Image(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "Photo Button",
                                    modifier = Modifier.size(40.dp)
                                )

                            }

                            Button(
                                onClick = { // add medicine
                                    if (networkStateViewModel.isOnline(context) && !isGuestUser) {
                                        pharmaciesViewModel.activateAddMedicine()
                                        medicines = medicinesAdd
                                    }
                                    else if (isGuestUser) {
                                        Toast.makeText(
                                            context,
                                            "Guest users cannot use medicines",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else {
                                        Toast.makeText(
                                            context,
                                            "No internet connection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(60.dp)
                            )
                            {
                                Text(
                                    text = stringResource(id = R.string.scan_barcode_add),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center

                                )
                            }

                            Button(
                                onClick = { // reduce medicine
                                    if (networkStateViewModel.isOnline(context) && !isGuestUser) {
                                        pharmaciesViewModel.activateUseMedicine()
                                        medicines = medicinesReduce

                                    }
                                    else if (isGuestUser) {
                                        Toast.makeText(
                                            context,
                                            "Guest users cannot use medicines",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    else {
                                        Toast.makeText(
                                            context,
                                            "No internet connection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(60.dp)
                                    .align(Alignment.BottomStart)
                            )
                            {
                                Text(
                                    text = stringResource(id = R.string.scan_barcode_reduce),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }


                } else {
                    Log.d("PharmacyPanelScreen", "Pharmacy not found for ID: $pharmacyID")
                    Text(
                        text = "Pharmacy not found",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )

    // Clean up resources when the composable is no longer in use
    DisposableEffect(Unit) {
        onDispose {
            Log.d("PharmacyPanelScreen", "Disposing resources")
            // Cancel any ongoing work or release resources here
        }
    }
}


// Function to create the button to scan the barcode and the text that contains the barcode value
@Composable
private fun ScanBarcode(
    onScanBarcode: suspend () -> Unit,
    barcodeValue: String?,
) {
    val scope = rememberCoroutineScope()

    Button(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.grey),
            contentColor = Color.Black
        ),
        onClick = {
            scope.launch {
                onScanBarcode()
            }
        }) {
        Text(
            text = "Scan Barcode",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = colorResource(id = R.color.black),
        )
    }
    // Display the barcode value
    Text(
        text = barcodeValue ?: "0000000",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimary
    )
}

// Function to create the actual add quantity pop up menu
@Composable
fun createPopUpMenuToAddQuantity(
    context: Context,
    pharmacyID: String,
    pharmaciesViewModel: PharmaciesViewModel
): Map<String, Medicine> {

    val barcodeScanner = BarcodeScanner(context)

    var barcode by remember { mutableStateOf("") }

    val pharmacy = pharmaciesViewModel.getPharmacyById(pharmacyID)

    var quantity by remember { mutableStateOf("") }

    val addMedicine by pharmaciesViewModel.addMedicine.collectAsState()


    val contentForPopUp = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            //ask to read the barcode
            //Barcode Scanner implementation
            val barcodeResults =
                barcodeScanner.barCodeResult.collectAsStateWithLifecycle()

            ScanBarcode(
                onScanBarcode = {
                    barcodeScanner.scanBarcode { barcodeValue ->
                        if (barcodeValue != null) {
                            barcode = barcodeValue
                        }
                    }
                },
                barcodeValue = barcodeResults.value
            )
            //Get name of medicine
            Text(
                text = "Medicine Name: ${pharmaciesViewModel.getMedicineByBarcode(barcode)?.name}",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            //create a bar field to enter the quantity
            QuantityInputField(quantity = quantity, onQuantityChange = { quantity = it })


            //create button to add that quantity to the associated barcode in the pharmacy
            Button(
                onClick = {
                    if (pharmaciesViewModel.verifyIfBarcodeExists(barcode)) {

                        val medicine = pharmaciesViewModel.getMedicineByBarcode(barcode)
                        Log.d("PharmacyPanelScreen", "Medicine: $medicine")

                        if (quantity.toInt() < 0) {
                            Toast.makeText(
                                context,
                                "Quantity cannot be negative",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            if (!pharmaciesViewModel.verifyIfPharmacyIdExistsInTheMedicine(
                                    medicine!!,
                                    pharmacyID
                                ) && quantity.isNotEmpty() && quantity.toInt() > 0
                            ) {
                                //Add the pharmacy id to the medicine
                                pharmaciesViewModel.addPharmacyIdToMedicine(medicine, pharmacyID)
                            }

                            pharmacy?.addMedicineQuantity(barcode, quantity.toInt())

                            pharmaciesViewModel.updatePharmacyInBackend(pharmacy!!)
                            //Put the variables null
                            barcode = ""
                            quantity = ""

                            pharmaciesViewModel.deactivateAddMedicine()
                        }



                    } else {
                        Toast.makeText(
                            context,
                            "Medicine with this barcode does not exist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Add Quantity",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }

            Button(
                onClick = {
                    pharmaciesViewModel.deactivateAddMedicine()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }

        }

    }
    PopupBox(
        popupWidth = 300F,
        popupHeight = 300F,
        showPopup = addMedicine,
        content = contentForPopUp
    )
    return pharmaciesViewModel.getAllMedicinesOfPharmacy(pharmacyID)
}


// Function to create the actual remove quantity pop up menu
@Composable
fun createPopUpMenuToRemoveQuantity(
    context: Context,
    pharmacyID: String,
    pharmaciesViewModel: PharmaciesViewModel
): Map<String, Medicine> {

    val barcodeScanner = BarcodeScanner(context)

    var barcode by remember { mutableStateOf("") }

    val pharmacy = pharmaciesViewModel.getPharmacyById(pharmacyID)

    var quantity by remember { mutableStateOf("") }

    val removeMedicine by pharmaciesViewModel.removeMedicine.collectAsState()


    val contentForPopUp = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            //ask to read the barcode
            //Barcode Scanner implementation
            val barcodeResults =
                barcodeScanner.barCodeResult.collectAsStateWithLifecycle()

            ScanBarcode(
                onScanBarcode = {
                    barcodeScanner.scanBarcode { barcodeValue ->
                        if (barcodeValue != null) {
                            barcode = barcodeValue
                        }
                    }
                },
                barcodeValue = barcodeResults.value
            )
            //Get name of medicine
            Text(
                text = "Medicine Name: ${pharmaciesViewModel.getMedicineByBarcode(barcode)?.name}",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            //create a bar field to enter the quantity
            QuantityInputField(quantity = quantity, onQuantityChange = { quantity = it })


            //create button to remove that quantity to the associated barcode in the pharmacy
            Button(
                onClick = {

                    if (quantity.toInt() < 0) {
                        Toast.makeText(
                            context,
                            "Quantity cannot be negative",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (pharmaciesViewModel.verifyIfBarcodeExists(barcode) &&
                        pharmaciesViewModel.verifyIfPharmacyIdExistsInTheMedicine(
                            pharmaciesViewModel.getMedicineByBarcode(barcode)!!,
                            pharmacyID
                        ) && quantity.isNotEmpty() && quantity.toInt() > 0
                    ) {

                        /*Boolean to check if the quantity can be removed
                        * if so then remove the quantity from the pharmacy
                        */

                        val appliableRemove = pharmacy?.removeMedicineQuantity(barcode, quantity.toInt())

                        //If the quantity is not enough for the remove operation
                        if (!appliableRemove!!) {
                            Toast.makeText(
                                context,
                                "Medicine does not have enough quantity",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Remove the pharmacy id from the medicine if the quantity is less then 0
                            if (pharmacy.getMedicineQuantity(barcode)!! == 0) {
                                pharmaciesViewModel.removePharmacyIdFromMedicine(
                                    pharmaciesViewModel.getMedicineByBarcode(
                                        barcode
                                    )!!, pharmacyID
                                )
                                pharmaciesViewModel.removeMedicineFromPharmacy(
                                    pharmaciesViewModel.getMedicineByBarcode(
                                        barcode
                                    )!!, pharmacyID
                                )
                            }

                            pharmaciesViewModel.updatePharmacyInBackend(pharmacy)
                            //Put the variables null
                            barcode = ""
                            quantity = ""

                            pharmaciesViewModel.deactivateUseMedicine()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Medicine name null then Medicine does not exist else Medicine does not have stock ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Use Quantity",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }


            Button(
                onClick = {
                    pharmaciesViewModel.deactivateUseMedicine()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }

        }

    }
    PopupBox(
        popupWidth = 300F,
        popupHeight = 300F,
        showPopup = removeMedicine,
        content = contentForPopUp
    )
    return pharmaciesViewModel.getAllMedicinesOfPharmacy(pharmacyID)
}

//Create a text field to enter the quantity
@Composable
fun QuantityInputField(quantity: String, onQuantityChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = quantity,
        onValueChange = onQuantityChange,
        label = { Text("Enter Quantity") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.focusRequester(focusRequester)
    )
}

//Fuction to create the actual flag pharmacy pop up menu to confirm the flag or cancel the flag
@Composable
fun CreatePopMenuToConfirmFlag(
    pharmacyID: String,
    pharmaciesViewModel: PharmaciesViewModel,
    onFlagClick: (String) -> Unit
){
    val flagPharmacyMenu by pharmaciesViewModel.flagPharmacyMenu.collectAsState()

    val contentForPopUp = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Text(
                text = stringResource(id = R.string.are_you_sure),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = {
                    pharmaciesViewModel.deactivateFlagPharmacyMenu()
                    onFlagClick(pharmacyID)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Confirm ",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }

            Button(
                onClick = {
                    pharmaciesViewModel.deactivateFlagPharmacyMenu()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.grey),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(55.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp), // Set the button shape
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    color = Color.Black
                )
            }

        }

    }
    PopupBox(
        popupWidth = 230F,
        popupHeight = 230F,
        showPopup = flagPharmacyMenu,
        content = contentForPopUp
    )
}

