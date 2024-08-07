package com.ist.pharmacist.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.ist.pharmacist.MedicinePreferences
import com.ist.pharmacist.R
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.views.NetworkStateViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.ui.views.PhotoCacheViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinePanelScreen(
    context: Context,
    backHandler: () -> Unit,
    pharmaciesViewModel: PharmaciesViewModel,
    photoCacheViewModel: PhotoCacheViewModel,
    networkStateViewModel: NetworkStateViewModel,
    barcode: String,
    user: User,
    onPharmacyButtonClick: (String) -> Unit,
    onNotifyUser: () -> Unit,
) {
    val isGuest = (user.username == "Guest")

    val medicine = remember { pharmaciesViewModel.getMedicineByBarcode(barcode) }
    Log.d("Medicine", medicine.toString())

    // Search for pharmacies that have the medicine
    var searchResults by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(medicine) {
        if (medicine != null) {
            searchResults = pharmaciesViewModel.searchPharmaciesWithMedicine(medicine, context)
        }
    }

    // Handle medicine photo
    val medicinePreferences = MedicinePreferences(context)
    val coroutineScope = rememberCoroutineScope()

    if (medicine != null) {
        var isFavorite by remember { mutableStateOf(medicine.isUserToNotify(user.username)) }

        val inriaSerifFont = FontFamily(
            Font(R.font.inria_serif_regular),
        )

        // Photo Handling
        val isMeteredConnection by networkStateViewModel.isMeteredConnection.collectAsState()
        var medicinePhotoUrl by remember { mutableStateOf<String?>(null) }
        var hasLocalPhoto by remember { mutableStateOf(false) }
        var localPhotoPath by remember { mutableStateOf<String?>(null) }

        val medicinePhotoCache = photoCacheViewModel.medicinePhotoCache

        LaunchedEffect(barcode) {
            coroutineScope.launch {
                Log.d("MedicinePanelScreen", "Checking for local medicine photo")
                hasLocalPhoto = withContext(Dispatchers.IO) {
                    medicinePreferences.hasMedicinePhoto(barcode)
                }
                localPhotoPath = withContext(Dispatchers.IO) {
                    medicinePreferences.getMedicinePhotoPath(barcode)
                }
            }
        }

        // Observe network state changes
        LaunchedEffect(isMeteredConnection) {
            if (!isMeteredConnection && medicinePhotoUrl == null && !hasLocalPhoto) {
                // Load photo from cache or fetch from backend
                if (medicinePhotoCache.containsKey(barcode)) {
                    medicinePhotoUrl = medicinePhotoCache[barcode]
                } else {
                    // Fetch the medicine photo from the backend
                    val photoUrl = pharmaciesViewModel.fetchMedicinePhotosFromBackend(barcode)
                    if (photoUrl != null) {
                        medicinePhotoUrl = photoUrl
                        photoCacheViewModel.cacheMedicinePhoto(barcode, medicinePhotoUrl!!)
                    }
                }
            }
        }


        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(id = R.color.green),
                        titleContentColor = colorResource(id = R.color.white),
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Spacer(modifier = Modifier.width(8.dp)) // Space between image and text
                            Text(
                                stringResource(id = R.string.medicine_panel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },

                    navigationIcon = {
                        IconButton(onClick = {
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
                        .padding(padding)
                        .offset(y = 10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (isGuest) {
                                    makeText(
                                        context,
                                        "Please sign in to use this feature",
                                        LENGTH_SHORT
                                    ).show()
                                    return@IconButton
                                }
                                else {
                                    // Handle favorite state change
                                    isFavorite = !isFavorite
                                    onNotifyUser()
                                }
                            },
                            modifier = Modifier
                                .size(60.dp)
                                .padding(start = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isFavorite) R.drawable.bell_on else R.drawable.bell_off
                                ),
                                contentDescription = if (isFavorite) "Favorite" else "Not Favorite",
                                tint = Color.Black
                            )
                        }

                        Text(
                            text = medicine.name,
                            fontSize = 24.sp,
                            fontFamily = inriaSerifFont,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .fillMaxWidth()
                                .offset(x = (-26).dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))


                    // Display the medicine photo
                    val imageSize = 175.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (hasLocalPhoto && localPhotoPath != null) {
                            Box(
                                modifier = Modifier
                                    .size(imageSize)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(localPhotoPath),
                                    contentDescription = "Medicine Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else if (medicinePhotoUrl != null || medicinePhotoCache.containsKey(
                                barcode
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(imageSize)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = medicinePhotoUrl ?: medicinePhotoCache[barcode],
                                    contentDescription = "Medicine Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(imageSize)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .clickable {
                                        if (networkStateViewModel.isOnline(context)) {
                                            if (isMeteredConnection) {
                                                coroutineScope.launch {
                                                    val photoUrl =
                                                        pharmaciesViewModel.fetchMedicinePhotosFromBackend(
                                                            barcode
                                                        )
                                                    if (photoUrl != null) {
                                                        medicinePhotoUrl = photoUrl
                                                        photoCacheViewModel.cacheMedicinePhoto(
                                                            barcode,
                                                            photoUrl
                                                        )
                                                    }
                                                }
                                            } else {
                                                medicinePhotoUrl = medicine.box_photo_path
                                                photoCacheViewModel.cacheMedicinePhoto(
                                                    barcode,
                                                    medicinePhotoUrl!!
                                                )
                                            }
                                        }
                                        else {
                                            makeText(
                                                context,
                                                "No internet connection",
                                                LENGTH_SHORT
                                            ).show()
                                        }

                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap to load photo",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val state = rememberLazyListState()
                        LazyColumn(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.8f)
                                .weight(1f)
                                .scrollbar(state, horizontal = false),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Log.d("SearchResults", searchResults.toString())
                            items(searchResults) { result ->
                                val (pharmacyID, distance) = result.split(" - ")
                                val pharmacyName =
                                    pharmaciesViewModel.getPharmacy(pharmacyID)?.name
                                Button(
                                    onClick = {
                                        // Navigate to pharmacy panel screen
                                        onPharmacyButtonClick(pharmacyID)
                                    },
                                    contentPadding = PaddingValues(8.dp),
                                    modifier = Modifier
                                        .width(300.dp)
                                        .height(92.dp)
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.grey),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.pharmacy_marker),
                                            contentDescription = "Medicine Icon",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Black
                                        )
                                        if (pharmacyName != null) {
                                            Text(
                                                text = pharmacyName,
                                                fontSize = 15.sp,
                                                fontFamily = inriaSerifFont,
                                                color = Color.Black,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = distance,
                                            fontSize = 12.sp,
                                            fontFamily = inriaSerifFont,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }

            }
        )
    }
}