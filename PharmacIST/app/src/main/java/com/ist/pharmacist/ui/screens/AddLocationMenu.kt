package com.ist.pharmacist.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ist.pharmacist.R
import com.ist.pharmacist.ui.theme.MapStyle
import com.ist.pharmacist.ui.views.FocusAddressViewModel
import com.ist.pharmacist.ui.views.MarkerLocationsViewModel
import com.ist.pharmacist.utils.checkForPermission
import com.ist.pharmacist.utils.fetchPlaceDetails
import com.ist.pharmacist.utils.getCurrentLocation
import kotlinx.coroutines.launch

// AddLocationScreen composable which is used to add a location to the map
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationScreen(
    context: Context,
    backHandler: () -> Unit,
    markerLocationsViewModel: MarkerLocationsViewModel,
    focusAddressViewModel: FocusAddressViewModel,
    navigateToAddPharmacy: (LatLng) -> Unit,
) {

    // Variables for the map
    val coroutineScope = rememberCoroutineScope()
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var newLocation by remember { mutableStateOf<LatLng?>(null) }
    val latLngList by markerLocationsViewModel.markerLocations.observeAsState(emptyList())
    val currentLatLngList = remember { mutableStateListOf<LatLng>() }

    val cameraPositionState = rememberCameraPositionState()

    // Search bar variables
    val text = remember { mutableStateOf(TextFieldValue()) }
    var searchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val placesClient = Places.createClient(context)
    var addressList by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }

    val focusLocation by focusAddressViewModel.location.observeAsState(null)


    // Focus on the current location when the screen is launched
    LaunchedEffect(focusLocation) {
        if (focusLocation == null) {
            if (checkForPermission(context)) {
                getCurrentLocation(context)?.let {
                    focusAddressViewModel.updateLocation(it)
                }
            }
        }
        focusLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(it))
            cameraPositionState.animate(CameraUpdateFactory.zoomTo(17f))
            searchActive = false
        }
    }

    //Ui for the AddLocationScreen
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
                            stringResource(id = R.string.add_location),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = backHandler) {
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
                latLngList.forEach { latLng ->
                    if (!currentLatLngList.contains(latLng))
                        currentLatLngList.add(latLng)
                }

                //Map where the new marker will be placed
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.matchParentSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = {
                            if (currentLatLngList.size == latLngList.size) {
                                if (!currentLatLngList.contains(it)) {
                                    currentLatLngList.add(it)
                                }
                            } else if (!currentLatLngList.contains(it) && currentLatLngList.size == latLngList.size + 1) {
                                currentLatLngList.removeAt(currentLatLngList.size - 1)
                                currentLatLngList.add(it)
                            }
                        },
                        properties = MapProperties(
                            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                                context,
                                MapStyle.mapStyle
                            )
                        ),
                    ) {
                        currentLatLngList.toList().forEach {
                            Marker(
                                state = MarkerState(position = it),
                                icon = bitmapDescriptorWithSizeAndColor(
                                    context,
                                    R.drawable.pharmacy_marker,
                                    125,
                                    125,
                                    ContextCompat.getColor(context, R.color.green)
                                ),
                                onClick = {false}
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        getCurrentLocation(context)?.let {
                                            currentLocation = it
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    currentLocation!!,
                                                    15f
                                                )
                                            )
                                            if (!currentLatLngList.contains(currentLocation) && currentLatLngList.size == latLngList.size) {
                                                currentLatLngList.add(currentLocation!!)
                                            }
                                            if (!currentLatLngList.contains(currentLocation) && currentLatLngList.size == latLngList.size + 1) {
                                                currentLatLngList.removeAt(currentLatLngList.size - 1)
                                                currentLatLngList.add(currentLocation!!)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.current_location_icon_png_13),
                                    contentDescription = "Move to Current Location",
                                    modifier = Modifier.size(47.dp),
                                    colorFilter = ColorFilter.tint(Color.DarkGray)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentLatLngList.size > latLngList.size) {
                                        newLocation = currentLatLngList[currentLatLngList.size - 1]
                                        currentLatLngList.clear()
                                        navigateToAddPharmacy(newLocation!!)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                                    .padding(8.dp)

                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "Add Location",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(colorResource(id = R.color.black))
                                )
                            }

                            // Search bar UI, logic and Handle the autocomplete predictions
                            val containerColor = MaterialTheme.colorScheme.primaryContainer
                            TextField(
                                value = text.value,
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp),
                                interactionSource = remember { MutableInteractionSource() }
                                    .also { interactionSource ->
                                        LaunchedEffect(interactionSource) {
                                            interactionSource.interactions.collect {
                                                if (it is PressInteraction.Release) {
                                                    searchActive = true
                                                }
                                            }
                                        }
                                    },
                                onValueChange = { newValue ->
                                    text.value = newValue
                                    searchActive = true

                                    // Create a token for the autocomplete session
                                    val token = AutocompleteSessionToken.newInstance()

                                    // create a request for autocomplete predictions
                                    val request = FindAutocompletePredictionsRequest.builder()
                                        .setCountries("PT") // restricted to Portugal
                                        .setTypesFilter(listOf(PlaceTypes.ADDRESS)) // restrict to addresses
                                        .setSessionToken(token)
                                        .setQuery(newValue.text)
                                        .build()

                                    // AutoComplete request
                                    placesClient.findAutocompletePredictions(request)
                                        .addOnSuccessListener { response ->
                                            addressList = response.autocompletePredictions.take(2)
                                        }
                                        .addOnFailureListener { exception ->
                                            if (exception is ApiException) {
                                                Log.e(
                                                    "Places",
                                                    "Place not found: " + exception.statusCode
                                                )
                                            }
                                        }
                                },
                                placeholder = {
                                    Text(
                                        text = "Search address...",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.offset(x = (38).dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 50.dp, end = 50.dp, top = 0.dp, bottom = 4.dp)
                                    .align(Alignment.BottomCenter)
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp, 25.dp, 25.dp, 25.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Icon Search",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .offset(x = (14).dp)
                                    )
                                },
                                trailingIcon = {
                                    Log.i("searchActive", searchActive.toString())
                                    if (searchActive || text.value.text.isNotEmpty()) {
                                        Icon(
                                            modifier = Modifier
                                                .clickable {
                                                    if (text.value.text.isEmpty()) {
                                                        keyboardController?.hide()
                                                        searchActive = false
                                                        focusManager.clearFocus()

                                                    } else {
                                                        text.value = TextFieldValue()
                                                        addressList = emptyList()

                                                    }
                                                },
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close Icon"
                                        )
                                    }

                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        searchActive = false
                                        focusManager.clearFocus()
                                    }),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    disabledTextColor = Color.Transparent,
                                    errorTextColor = Color.Black,
                                    focusedContainerColor = containerColor,
                                    unfocusedContainerColor = containerColor,
                                    disabledContainerColor = containerColor,
                                    cursorColor = Color.Black,
                                    errorCursorColor = Color.Black,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    errorIndicatorColor = Color.Transparent,
                                    focusedPlaceholderColor = Color.Black,
                                    unfocusedPlaceholderColor = Color.Black,
                                )
                            )

                            // Autocomplete predictions
                            if (addressList.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 50.dp,
                                            end = 50.dp,
                                            top = 0.dp,
                                            bottom = 0.dp
                                        )
                                        .align(Alignment.BottomCenter)
                                        .offset(y = (-60).dp)
                                ) {
                                    items(addressList.take(2)) { address ->
                                        Button(
                                            onClick = {
                                                val placeId = address.placeId
                                                coroutineScope.launch {
                                                    fetchPlaceDetails(context, placeId) { latLng ->
                                                        focusAddressViewModel.updateLocation(latLng)
                                                        searchActive = false
                                                        addressList = emptyList()
                                                        text.value = TextFieldValue()
                                                        keyboardController?.hide()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .height(60.dp),
                                            shape = RoundedCornerShape(25.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.List,
                                                contentDescription = "Icon List",
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .offset(x = (-8).dp)
                                            )
                                            Text(
                                                text = address.getFullText(null).toString(),
                                                fontSize = 15.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .offset(x = (-12).dp)
                                            )
                                        }
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

