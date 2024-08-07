package com.ist.pharmacist.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ist.pharmacist.ComposeFileProvider
import com.ist.pharmacist.PharmacyPreferences
import com.ist.pharmacist.R
import com.ist.pharmacist.data.Pharmacy
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.theme.MapStyle
import com.ist.pharmacist.ui.views.AddPharmacyViewModel
import com.ist.pharmacist.ui.views.MarkerLocationsViewModel
import com.ist.pharmacist.ui.views.NewMarkerLocationViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.utils.getAddressFromLatLng
import com.ist.pharmacist.utils.isFieldValid
import com.ist.pharmacist.utils.isImageValid
import com.ist.pharmacist.utils.saveImageToInternalStorage
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPharmacyScreen(
    context: Context,
    backHandler: () -> Unit,
    addLocationHandler: () -> Unit,
    goToMainMenu: () -> Unit,
    markerLocationsViewModel: MarkerLocationsViewModel,
    newMarkerLocation: NewMarkerLocationViewModel,
    addPharmacyViewModel: AddPharmacyViewModel,
    pharmaciesViewModel: PharmaciesViewModel,
    user: User
)
{

    val pharmacyName by addPharmacyViewModel.pharmacyName.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val confirmedImageUri by addPharmacyViewModel.confirmedImageUri.collectAsState()
    val showImageSelectionButtons by addPharmacyViewModel.showImageSelectionButtons.collectAsState()


    var isPharmacyNameError by remember { mutableStateOf(false) }

    val newLocation by newMarkerLocation.location.observeAsState(null)

    var cameraCapturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Activity Result Launchers to handle camera and photo picker
    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraCapturedImageUri != null) {
                addPharmacyViewModel.updateConfirmedImageUri(cameraCapturedImageUri)
                addPharmacyViewModel.updateShowImageSelectionButtons(false)
            }
        }
    // Activity Result Launcher to handle photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            showConfirmationDialog = true
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // Permission granted, launch the camera
                val imageUri = ComposeFileProvider.getImageUri(context)
                cameraCapturedImageUri = imageUri
                takePictureLauncher.launch(imageUri)
            } else {
                // Permission denied
                Toast.makeText(
                    context,
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    val pharmacyPreferences = remember { PharmacyPreferences(context) }
    val confirmedImagePath by addPharmacyViewModel.confirmedImagePath.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(newLocation ?: LatLng(0.0, 0.0), 15f)
    }

    LaunchedEffect(newLocation) {
        newLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                durationMs = 500
            )
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
                            stringResource(id = R.string.add_pharmacy),
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
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height( if(newLocation == null) 140.dp else 50.dp))
                EditTextBarField(
                    label = stringResource(id = R.string.pharmacy_name),
                    value = pharmacyName,
                    hide = false,
                    onValueChange = { addPharmacyViewModel.updatePharmacyName(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 10.dp)
                        .fillMaxWidth(),
                    isError = isPharmacyNameError,
                    supportingText = if (isPharmacyNameError) stringResource(id = R.string.invalid_pharmacy_name) else ""
                )

                if(newLocation == null) {
                    Button(
                        onClick = { addLocationHandler() },
                        modifier = Modifier
                            .size(300.dp, 50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .padding(bottom = 10.dp)
                            .fillMaxWidth(),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),

                        ) {
                        Text(text = stringResource(id = R.string.add_location))
                    }
                }else{
                    Box(
                        modifier = Modifier
                            .size(300.dp, 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .padding(bottom = 10.dp)
                    ){
                        GoogleMap(
                            modifier = Modifier.matchParentSize(),
                            cameraPositionState = cameraPositionState,
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
                                state = MarkerState(position = newLocation ?: LatLng(0.0, 0.0)),
                                title = "Selected Location",
                                snippet = "Marker at the selected location",
                                icon = bitmapDescriptorWithSizeAndColor(
                                    context,
                                    R.drawable.pharmacy_marker,
                                    125,
                                    125,
                                    ContextCompat.getColor(context, R.color.green)
                                )
                            )
                        }
                        IconButton(
                            onClick = {
                                addLocationHandler() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Location",
                                tint = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
                // Add photo button
                Button(
                    onClick = { // add photo button
                        if (confirmedImagePath != null) {
                            // Handle the case where the pharmacy photo is already set ( ask for confirmation to change the photo)
                            showBottomSheet = true
                            addPharmacyViewModel.updateConfirmedImageUri(null)
                            addPharmacyViewModel.updateShowImageSelectionButtons(true)
                            scope.launch {
                                sheetState.show()
                            }
                        } else {

                            //generate a temporary ID for the pharmacy
                            val tempPharmacyID = UUID.randomUUID().toString()
                            addPharmacyViewModel.updateTempPharmacyID(tempPharmacyID)


                            showBottomSheet = true
                            scope.launch {
                                sheetState.show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.grey),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 5.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    // Display the pharmacy photo if it exists
                    if (confirmedImagePath != null) {
                        Image(
                            painter = rememberAsyncImagePainter(confirmedImagePath),
                            contentDescription = "Photo Button",
                            modifier = Modifier.fillMaxHeight().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Display the default photo button image
                        Image(
                            painter = painterResource(id = R.drawable.photo_button_image),
                            contentDescription = "Photo Button",
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
                Button(
                    onClick = {
                        isPharmacyNameError = !isFieldValid(pharmacyName)

                        if (!isPharmacyNameError && newLocation != null && isImageValid(confirmedImagePath)) {
                            if (!markerLocationsViewModel.containsLocation(newLocation!!)) {
                                markerLocationsViewModel.addMarkerLocation(newLocation!!)

                                // Save the pharmacy data on the pharmaciesViewModel
                                val addressLocation = getAddressFromLatLng(context, newLocation!!)

                                val newPharmacy = Pharmacy("", user.username, pharmacyName, addressLocation, newLocation!!, "")

                                // Assign the temporary ID to the new_pharmacy object
                                val tempPharmacyId = addPharmacyViewModel.tempPharmacyID.value
                                if (tempPharmacyId != null) {
                                    newPharmacy.id = tempPharmacyId
                                }

                                scope.launch {
                                    // Update the photo path with the final pharmacy ID
                                    if (tempPharmacyId != null) {
                                        val imagePath = pharmacyPreferences.getPharmacyPhotoPath(tempPharmacyId)
                                        Log.d("AddPharmacyScreen", "Image Path: $imagePath")
                                        if (imagePath != null) {
                                            // Update the photo path in DataStore using the final pharmacy ID
                                            pharmacyPreferences.savePharmacyPhotoPath(newPharmacy.id, imagePath)
                                            newPharmacy.setPhotoPath(imagePath)
                                            pharmaciesViewModel.addPharmacy(newPharmacy)
                                            pharmaciesViewModel.addPharmacyToListOfPharmaciesCreatedByUser(newPharmacy.id, user)
                                        }
                                    }
                                    Log.d("AddPharmacyScreen", "New Pharmacy: $newPharmacy")

                                    // add the pharmacy to the backend with ID as document name
                                    pharmaciesViewModel.addPharmacyBackend(newPharmacy)
                                }

                                // Reset the screen to its initial state for new additions
                                addPharmacyViewModel.uploadFileToFirebaseStorage()
                                resetPharmacyData(addPharmacyViewModel, newMarkerLocation)

                                goToMainMenu()
                            } else {
                                Toast.makeText(context, "Pharmacy with this location already exists", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (!isImageValid(confirmedImagePath)) {
                                Toast.makeText(context, "Please add a pharmacy photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, // add final pharmacy button
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.grey),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 5.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Photo Button",
                        modifier = Modifier.fillMaxSize()
                    )
                }

            }

            // Bottom Sheet for adding a photo when clocked on the photo button
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    modifier = if (confirmedImageUri != null) Modifier.fillMaxHeight(0.8f)
                    else Modifier.fillMaxHeight(0.3f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add Photo Menu",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (showConfirmationDialog && selectedImageUri != null) {
                            AlertDialog(
                                onDismissRequest = { showConfirmationDialog = false },
                                title = { Text("Confirm Image") },
                                text = {
                                    Image(
                                        painter = rememberAsyncImagePainter(selectedImageUri),
                                        contentDescription = "Selected Image",
                                        modifier = Modifier.size(200.dp)
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            addPharmacyViewModel.updateConfirmedImageUri(selectedImageUri)
                                            showConfirmationDialog = false
                                            addPharmacyViewModel.updateShowImageSelectionButtons(false)
                                            addPharmacyViewModel.updateConfirmedImagePath(null)
                                        }
                                    ) {
                                        Text("Confirm")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            selectedImageUri = null
                                            showConfirmationDialog = false
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        if (showImageSelectionButtons) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        // Handle add from files logic
                                        photoPickerLauncher.launch(PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.grey),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.folder),
                                        contentDescription = "Add from Files",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .padding(bottom = 3.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        // Handle add from camera logic
                                        permissionsLauncher.launch(Manifest.permission.CAMERA)
                                        addPharmacyViewModel.updateConfirmedImagePath(null)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.grey),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.photo_button_image),
                                        contentDescription = "Add from Camera",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .padding(bottom = 3.dp)
                                    )
                                }
                            }
                        } else {
                            if (confirmedImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(confirmedImageUri),
                                    contentDescription = "Confirmed Image",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .fillMaxSize()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(
                                        onClick = {
                                            // Process canceling the image
                                            addPharmacyViewModel.updateConfirmedImageUri(null)
                                            addPharmacyViewModel.updateShowImageSelectionButtons(true)
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.x),
                                            contentDescription = "Cancel Image Selection",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(bottom = 3.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            // Process confirming the image
                                            scope.launch {
                                                // Save the confirmed image to internal storage using the temporary pharmacy ID
                                                val file = saveImageToInternalStorage(
                                                    context,
                                                    confirmedImageUri!!,
                                                    addPharmacyViewModel.tempPharmacyID.value!!
                                                )
                                                if (file != null) {
                                                    // Save the image path to DataStore using the temporary pharmacy ID
                                                    pharmacyPreferences.savePharmacyPhotoPath(addPharmacyViewModel.tempPharmacyID.value!!, file.absolutePath)

                                                    Log.d("AddPharmacyScreen", "Saved image path: ${file.absolutePath}")

                                                    // Update the UI with the saved image path
                                                    val imagePath = pharmacyPreferences.getPharmacyPhotoPath(addPharmacyViewModel.tempPharmacyID.value!!)
                                                    if (imagePath != null) {
                                                        addPharmacyViewModel.updateConfirmedImagePath(imagePath)
                                                        Log.d("AddPharmacyScreen", "Confirmed Image Path: $imagePath")
                                                    }
                                                }
                                            }
                                            showBottomSheet = false
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.check),
                                            contentDescription = "Add as Pharmacy Photo",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(bottom = 3.dp)
                                        )
                                    }

                                }
                            }
                        }
                    }
                }
            }

        }
    )
}

// Function to reset the screen to its initial state for new additions
fun resetPharmacyData(addPharmacyViewModel: AddPharmacyViewModel, newMarkerLocation: NewMarkerLocationViewModel) {
    // Reset the screen to its initial state for new additions
    addPharmacyViewModel.updatePharmacyName("")
    newMarkerLocation.location.value = null
    addPharmacyViewModel.updateConfirmedImageUri(null)
    addPharmacyViewModel.updateShowImageSelectionButtons(true)
    addPharmacyViewModel.updateConfirmedImagePath(null)
}
