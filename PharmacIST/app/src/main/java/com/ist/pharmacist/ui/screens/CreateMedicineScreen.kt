package com.ist.pharmacist.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.ist.pharmacist.ComposeFileProvider
import com.ist.pharmacist.R
import com.ist.pharmacist.data.Medicine
import com.ist.pharmacist.ui.views.AddMedicineViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.utils.BarcodeScanner
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ist.pharmacist.MedicinePreferences
import com.ist.pharmacist.utils.isFieldValid
import com.ist.pharmacist.utils.isImageValid
import com.ist.pharmacist.utils.saveImageToInternalStorage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMedicineScreen(
    context: Context,
    backHandler: () -> Unit,
    pharmacyID : String,
    pharmaciesViewModel: PharmaciesViewModel,
    addMedicineViewModel: AddMedicineViewModel
) {

    //Variables for the view model
    val pharmacy = pharmaciesViewModel.getPharmacy(pharmacyID)
    
    val medicinePreferences = remember { MedicinePreferences(context) }
    
    var showBottomSheet by remember { mutableStateOf(false) }

    val medicineName by addMedicineViewModel.medicineName.collectAsState()

    val quantity by addMedicineViewModel.quantity.collectAsState()

    val purpose by addMedicineViewModel.purpose.collectAsState()

    val barcode by addMedicineViewModel.barcode.collectAsState()

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()


    //Variables for image handling
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showConfirmationDialog by remember { mutableStateOf(false) }

    val confirmedImageUri by addMedicineViewModel.confirmedImageUri.collectAsState()

    val showImageSelectionButtons by addMedicineViewModel.showImageSelectionButtons.collectAsState()

    var cameraCapturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraCapturedImageUri != null) {
                addMedicineViewModel.updateConfirmedImageUri(cameraCapturedImageUri)
                addMedicineViewModel.updateShowImageSelectionButtons(false)
            }
        }

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

    val confirmedImagePath by addMedicineViewModel.confirmedImagePath.collectAsState()
    val barcodeScanner = BarcodeScanner(context)

    // Error handling for the form fields

    var isMedicineNameError by remember { mutableStateOf(false) }

    var isQuantityError by remember { mutableStateOf(false) }

    var isPurposeError by remember { mutableStateOf(false) }

    var isBarcodeError by remember { mutableStateOf(false) }

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
                            stringResource(id = R.string.create_medicine),
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
            ){
                Spacer(modifier = Modifier.height(50.dp))
                EditTextBarField(
                    label = stringResource(id = R.string.medicine_name),
                    value = medicineName,
                    hide = false,
                    onValueChange = { addMedicineViewModel.updateMedicineName(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 10.dp)
                        .fillMaxWidth(),
                    isError = isMedicineNameError,
                    supportingText = if (isMedicineNameError) stringResource(id = R.string.invalid_medicine_name) else ""


                )
                Spacer(modifier = Modifier.height(5.dp))
                EditTextBarField(
                    label = stringResource(id = R.string.quantity),
                    value = quantity,
                    hide = false,
                    onValueChange = { addMedicineViewModel.updateQuantity(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 10.dp)
                        .fillMaxWidth(),
                    isError = isQuantityError,
                    supportingText = if (isQuantityError) stringResource(id = R.string.invalid_quantity) else ""

                )
                Spacer(modifier = Modifier.height(5.dp))
                EditTextBarField(
                    label = stringResource(id = R.string.purpose_preferred),
                    value = purpose,
                    hide = false,
                    onValueChange = { addMedicineViewModel.updatePurpose(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 20.dp)
                        .fillMaxWidth(),
                    isError = isPurposeError,
                    supportingText = if (isPurposeError) stringResource(id = R.string.invalid_purpose) else ""

                )

                //Barcode Scanner implementation
                val barcodeResults =
                    barcodeScanner.barCodeResult.collectAsStateWithLifecycle()

                ScanBarcodeCreateMedicine(
                    onScanBarcode = {
                        barcodeScanner.scanBarcode { barcodeValue ->
                            if (barcodeValue != null) {
                                addMedicineViewModel.updateBarCode(barcodeValue)
                            }
                        }
                    },
                    barcodeResults.value,
                    addMedicineViewModel
                )

                Log.d("Barcode on the addMediceneViewModel", addMedicineViewModel.getBarCode())

                Button(
                    onClick = {
                        if (confirmedImagePath != null) {
                            // Handle the case where the medicine photo is already set (ask for confirmation to change the photo)
                            showBottomSheet = true
                            addMedicineViewModel.updateConfirmedImageUri(null)
                            addMedicineViewModel.updateShowImageSelectionButtons(true)
                            scope.launch {
                                sheetState.show()
                            }
                        } else {

                            showBottomSheet = true
                            scope.launch {
                                sheetState.show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 5.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (confirmedImagePath != null) {
                        Image(
                            painter = rememberAsyncImagePainter(confirmedImagePath),
                            contentDescription = "Photo Button",
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.photo_button_image),
                            contentDescription = "Photo Button",
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
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
                                                addMedicineViewModel.updateConfirmedImageUri(selectedImageUri)
                                                showConfirmationDialog = false
                                                addMedicineViewModel.updateShowImageSelectionButtons(false)
                                                addMedicineViewModel.updateConfirmedImagePath(null)
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
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                                            permissionsLauncher.launch(android.Manifest.permission.CAMERA)
                                            addMedicineViewModel.updateConfirmedImagePath(null)
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
                                                addMedicineViewModel.updateConfirmedImageUri(null)
                                                addMedicineViewModel.updateShowImageSelectionButtons(true)
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
                                                    // Save the confirmed image to internal storage using the barcode
                                                    val file = saveImageToInternalStorage(
                                                        context,
                                                        confirmedImageUri!!,
                                                        barcode
                                                    )
                                                    if (file != null) {
                                                        // Save the image path to DataStore using the barcode
                                                        medicinePreferences.saveMedicinePhotoPath(barcode, file.absolutePath)

                                                        Log.d("CreateMedicineScreen", "Saved image path: ${file.absolutePath}")

                                                        // Update the UI with the saved image path
                                                        val imagePath = medicinePreferences.getMedicinePhotoPath(barcode)
                                                        if (imagePath != null) {
                                                            addMedicineViewModel.updateConfirmedImagePath(imagePath)
                                                            Log.d("CreateMedicineScreen", "Confirmed Image Path: $imagePath")
                                                        }
                                                    }
                                                }
                                                showBottomSheet = false
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.check),
                                                contentDescription = "Add as Medicine Photo",
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
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = {
                        isMedicineNameError = !isFieldValid(medicineName)
                        isQuantityError = !isFieldValid(quantity)
                        isPurposeError = !isFieldValid(purpose)
                        isBarcodeError = !isFieldValid(barcode)

                        if (!isMedicineNameError && !isQuantityError && !isPurposeError && !isBarcodeError && isImageValid(confirmedImagePath)) {
                            if (!pharmaciesViewModel.verifyIfBarcodeExists(barcode)) {
                                // Construct the medicine object
                                val medicine = Medicine(medicineName, "", barcode, purpose)

                                medicine.addPharmacyId(pharmacyID)

                                pharmaciesViewModel.addMedicine(medicine)

                                val quantityInt = quantity.toInt()

                                scope.launch {
                                    // Update the photo path with the barcode
                                    val imagePath = medicinePreferences.getMedicinePhotoPath(barcode)
                                    Log.d("CreateMedicineScreen", "Image Path: $imagePath")
                                    if (imagePath != null) {
                                        // Update the photo path in DataStore using the barcode
                                        medicinePreferences.saveMedicinePhotoPath(barcode, imagePath)
                                        medicine.box_photo_path = imagePath
                                        // Add the medicine to the backend

                                        pharmaciesViewModel.addMedicineToBackend(medicine, onSuccess = {
                                            Log.d("CreateMedicineScreen", "Medicine added successfully")
                                            // Medicine added successfully, update the pharmacy document in Firestore
                                            if (pharmacy != null) {
                                                Log.d("CreateMedicineScreen", "Pharmacy: $pharmacy")
                                                pharmaciesViewModel.createMedicine(
                                                    medicine,
                                                    pharmacyID,
                                                    quantityInt
                                                )
                                                pharmaciesViewModel.updatePharmacyInBackend(pharmacy)
                                                pharmaciesViewModel.updateRepositories()

                                            }
                                        })
                                    }
                                    Log.d("CreateMedicineScreen", "New Medicine: $medicine")

                                    // Upload the medicine photo to Firebase Storage
                                    addMedicineViewModel.uploadFileToFirebaseStorage()

                                    // Reset the screen to its initial state for new additions
                                    resetMedicineData(addMedicineViewModel)

                                    // Navigate back to the previous screen
                                    backHandler()
                                }
                            } else {
                                Toast.makeText(context, "Medicine with this barcode already exists", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (!isImageValid(confirmedImagePath)) {
                                Toast.makeText(context, "Please add a medicine photo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    // add final pharmacy button
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.grey),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 5.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Photo Button",
                        modifier = Modifier.fillMaxSize()
                    )
                }

            }
            // Add Medicine Form


        }
    )
}

// Reset the medicine data after adding a new medicine so that the form is cleared
// for the next addition
private fun resetMedicineData(addMedicineViewModel: AddMedicineViewModel) {
    addMedicineViewModel.updateMedicineName("")
    addMedicineViewModel.updateQuantity("")
    addMedicineViewModel.updatePurpose("")
    addMedicineViewModel.updateBarCode("")
    addMedicineViewModel.updateConfirmedImageUri(null)
    addMedicineViewModel.updateShowImageSelectionButtons(true)
    addMedicineViewModel.updateConfirmedImagePath(null)
}



// Barcode Scanner implementation for adding a new medicine
@Composable
private fun ScanBarcodeCreateMedicine(
    onScanBarcode: suspend () -> Unit,
    barcodeValue: String?,
    addMedicineViewModel: AddMedicineViewModel
) {
    val scope = rememberCoroutineScope()


    // Scan Barcode Button
    Button(
        modifier = Modifier
            .fillMaxWidth(.80f)
            .size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            onClick = {
                scope.launch {
                    onScanBarcode()
                }
            }) {
            Text(
                text = "Scan Barcode",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                //style = TextStyle(fontWeight = FontWeight.Bold)
            )
        }

    Text(
        text = barcodeValue ?: "Scan barcode to add a medicine",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSecondary,
        fontSize = 20.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (barcodeValue != null) {
        addMedicineViewModel.updateBarCode(barcodeValue)
    }
}