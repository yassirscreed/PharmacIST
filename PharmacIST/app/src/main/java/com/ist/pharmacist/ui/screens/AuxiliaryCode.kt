package com.ist.pharmacist.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
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
import com.ist.pharmacist.data.Pharmacy
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.theme.MapStyle
import com.ist.pharmacist.ui.views.FocusAddressViewModel
import com.ist.pharmacist.ui.views.MarkerLocationsViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.utils.fetchPlaceDetails
import kotlinx.coroutines.launch

//Auxiliary code used in the project to create the UI components used in multiple screens

//EditTextBarField used to add a text field with a label to the screen
@Composable
fun EditTextBarField(
    label: String,
    value: String,
    hide: Boolean,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    isError: Boolean? = false,
    supportingText: String? = null,

    ) {

    val containerColor = colorResource(id = R.color.grey)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.6f
                })
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions.Default,
        singleLine = true,
        isError = isError ?: false,
        supportingText = { Text(supportingText ?: "") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            cursorColor = Color.Black,
            focusedBorderColor = MaterialTheme.colorScheme.onSecondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSecondary,
            focusedLeadingIconColor = colorResource(id = R.color.grey),
            focusedTrailingIconColor = colorResource(id = R.color.grey),
            unfocusedTrailingIconColor = colorResource(id = R.color.grey),
            focusedLabelColor = MaterialTheme.colorScheme.onSecondary,
            unfocusedLabelColor = Color.Black,
            focusedPlaceholderColor = colorResource(id = R.color.grey),
            unfocusedPlaceholderColor = colorResource(id = R.color.grey),
            focusedSupportingTextColor = MaterialTheme.colorScheme.onSecondary,
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSecondary,
            focusedPrefixColor = colorResource(id = R.color.grey),
            focusedSuffixColor = colorResource(id = R.color.grey),
        ),
        visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
    )
}

//Function to generate the map with the pharmacies for the main screen
@Composable
fun MyMap(
    context: Context,
    latLng: LatLng,
    mapProperties: MapProperties = MapProperties(),
    onMoveToCurrentLocation: () -> Unit,
    onPharmacyPanel: (String) -> Unit,
    pharmaciesViewModel: PharmaciesViewModel,
    onShowMoreResults: () -> Unit,
    focusAddressViewModel: FocusAddressViewModel,
    user: User,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 15f)
    }

    val coroutineScope = rememberCoroutineScope()
    val placesClient = Places.createClient(context)
    var addressList by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }

    val focusLocation by focusAddressViewModel.location.observeAsState(null)

    val text = remember { mutableStateOf(TextFieldValue()) }
    var searchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val bottomRoundedShape = RoundedCornerShape(25.dp, 25.dp, 25.dp, 25.dp)

    var toggleState by remember { mutableStateOf(false) }

    val searchResultsTemp = remember { mutableStateOf<List<Pharmacy>>(emptyList()) }
    val searchResults: State<List<Pharmacy>> = searchResultsTemp

    val pharmacies by pharmaciesViewModel.getPharmacies().observeAsState(emptyMap())




    /* Effect to focus the camera on the location of the address selected */
    LaunchedEffect(focusLocation) {
        focusLocation?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(it))
            cameraPositionState.animate(CameraUpdateFactory.zoomTo(17f))
            searchActive = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Remove pharmacies flagged by user and globally flagged pharmacies from the map  
        pharmaciesViewModel.removePharmaciesFlaggedFromMap(user)
        pharmaciesViewModel.removePharmaciesFlaggedGloballyFromMap()

        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties.copy(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, MapStyle.mapStyle)
            ),
        ) {
            pharmacies.forEach { (pharmacyID, pharmacy) ->
                val markerColor = if (user.isFavoritePharmacy(pharmacyID)) {
                    ContextCompat.getColor(
                        context,
                        com.google.android.libraries.places.R.color.quantum_yellow300
                    )
                } else {
                    ContextCompat.getColor(context, R.color.green)
                }
                Marker(
                    state = MarkerState(position = pharmacy.location),
                    title = pharmacy.name,
                    snippet = pharmacy.address,
                    icon = bitmapDescriptorWithSizeAndColor(
                        context,
                        R.drawable.pharmacy_marker,
                        125,
                        125,
                        markerColor
                    ),
                    onInfoWindowClick = {
                        coroutineScope.launch {
                            Log.d("MyMap", "Info window clicked for pharmacy: ${pharmacy.id}")
                            onPharmacyPanel(pharmacy.id)
                        }
                    }
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
                            onMoveToCurrentLocation()
                            cameraPositionState.animate(CameraUpdateFactory.newLatLng(latLng))
                            cameraPositionState.animate(CameraUpdateFactory.zoomTo(18f))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.current_location_icon_png_13), // Replace with your image resource
                        contentDescription = "Move to Current Location",
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(Color.DarkGray)
                    )
                }
                if (addressList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp, end = 50.dp, top = 0.dp, bottom = 0.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = (-60).dp)
                    ) {
                        if (addressList.size > 2) {
                            item {
                                Button(
                                    onClick = {
                                        onShowMoreResults()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(25.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.List,
                                        contentDescription = "Icon List",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .offset(y = (670).dp)
                                    )
                                    Text(
                                        text = "Show more results",
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .offset(x = (-12).dp)
                                    )
                                }
                            }
                        }

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
                if (toggleState && searchResults.value.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp, end = 50.dp, top = 0.dp, bottom = 0.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = (-60).dp)
                    ) {
                        items(searchResults.value.take(4)) { pharmacy ->
                            Button(
                                onClick = {
                                    onPharmacyPanel(pharmacy.id)
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
                                    text = pharmacy.name,
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

                val containerColor = MaterialTheme.colorScheme.primaryContainer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 50.dp, top = 0.dp, bottom = 4.dp)
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Switch(
                        checked = toggleState,
                        onCheckedChange = {
                            toggleState = it
                            if (toggleState) {
                                // switch to search for pharmacies
                                // Clear the current address list
                                addressList = emptyList()
                                // Reset the text field value
                                text.value = TextFieldValue()
                            } else {
                                // Revert back to search for addresses
                                // Clear the current search results
                                searchResultsTemp.value = emptyList()
                                // Reset the text field value
                                text.value = TextFieldValue()
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = containerColor
                        ),
                        thumbContent = {
                            if (toggleState) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                )

                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                )
                            }
                        }
                    )

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

                            if (toggleState) {
                                // Implement search for pharmacies here

                                if(newValue.text.isEmpty()){
                                    searchResultsTemp.value = emptyList()
                                } else {
                                    // Trigger the search for pharmacies based on the query
                                    val results = pharmaciesViewModel.searchPharmacies(newValue.text, pharmacies)
                                    searchResultsTemp.value = results
                                }


                            } else {
                                val token = AutocompleteSessionToken.newInstance()
                                val request = FindAutocompletePredictionsRequest.builder()
                                    .setCountries("PT")
                                    .setTypesFilter(listOf(PlaceTypes.ADDRESS))
                                    .setSessionToken(token)
                                    .setQuery(newValue.text)
                                    .build()

                                placesClient.findAutocompletePredictions(request)
                                    .addOnSuccessListener { response ->
                                        addressList = response.autocompletePredictions
                                    }
                                    .addOnFailureListener { exception ->
                                        if (exception is ApiException) {
                                            Log.e("Places", "Place not found: " + exception.statusCode)
                                        }
                                    }
                            }
                        },
                        placeholder = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = if (toggleState) "Search Pharmacies" else "Search address...",
                                    textAlign = TextAlign.Start,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 5.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = bottomRoundedShape,
                        leadingIcon = {
                            if (toggleState) {
                                Icon(
                                    imageVector = Icons.Filled.MedicalServices,
                                    contentDescription = null,
                                )

                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                )
                            }
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
                                                if(toggleState){
                                                    searchResultsTemp.value = emptyList()
                                                }


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
                            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledTextColor = Color.Transparent,
                            errorTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            focusedContainerColor = containerColor,
                            unfocusedContainerColor = containerColor,
                            disabledContainerColor = containerColor,
                            cursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            errorCursorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            }

        }
    }
}


// Create a BitmapDescriptor with the desired size and color for images in the drawable folder
fun bitmapDescriptorWithSizeAndColor(
    context: Context,
    id: Int,
    width: Int,
    height: Int,
    color: Int
): BitmapDescriptor {
    // Get the original drawable
    val originalDrawable = ContextCompat.getDrawable(context, id)

    // Create a mutable copy of the drawable
    val drawable = originalDrawable?.mutate()

    // Apply a color tint to the drawable
    DrawableCompat.setTint(drawable!!, color)
    DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN)

    // Create a bitmap with the desired dimensions
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Draw the tinted and resized drawable to the bitmap
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    // Convert the bitmap to a BitmapDescriptor
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

//Function to add the marker locations of the pharmacies to the map
fun addLocationsToViewModelFromPharmacies(
    pharmacies: MutableLiveData<Map<String, Pharmacy>>,
    viewModel: MarkerLocationsViewModel
) {
    pharmacies.value?.forEach { (_, pharmacy) ->
        viewModel.addMarkerLocation(pharmacy.location)
    }
}


/* Scrollbar */
@Composable
fun Modifier.scrollbar(
    state: LazyListState,
    horizontal: Boolean,
    alignEnd: Boolean = true,
    thickness: Dp = 4.dp,
    fixedKnobRatio: Float? = null,
    knobCornerRadius: Dp = 4.dp,
    trackCornerRadius: Dp = 2.dp,
    knobColor: Color = Color.Black,
    trackColor: Color = Color.White,
    padding: Dp = 0.dp,
    visibleAlpha: Float = 1f,
    hiddenAlpha: Float = 0f,
    fadeInAnimationDurationMs: Int = 150,
    fadeOutAnimationDurationMs: Int = 500,
    fadeOutAnimationDelayMs: Int = 1000,
): Modifier {
    check(thickness > 0.dp) { "Thickness must be a positive integer." }
    check(fixedKnobRatio == null || fixedKnobRatio < 1f) {
        "A fixed knob ratio must be smaller than 1."
    }
    check(knobCornerRadius >= 0.dp) { "Knob corner radius must be greater than or equal to 0." }
    check(trackCornerRadius >= 0.dp) { "Track corner radius must be greater than or equal to 0." }
    check(hiddenAlpha <= visibleAlpha) { "Hidden alpha cannot be greater than visible alpha." }
    check(fadeInAnimationDurationMs >= 0) {
        "Fade in animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDurationMs >= 0) {
        "Fade out animation duration must be greater than or equal to 0."
    }
    check(fadeOutAnimationDelayMs >= 0) {
        "Fade out animation delay must be greater than or equal to 0."
    }

    val targetAlpha =
        if (state.isScrollInProgress) {
            visibleAlpha
        } else {
            hiddenAlpha
        }
    val animationDurationMs =
        if (state.isScrollInProgress) {
            fadeInAnimationDurationMs
        } else {
            fadeOutAnimationDurationMs
        }
    val animationDelayMs =
        if (state.isScrollInProgress) {
            0
        } else {
            fadeOutAnimationDelayMs
        }

    val alpha by
    animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec =
        tween(delayMillis = animationDelayMs, durationMillis = animationDurationMs), label = ""
    )

    return drawWithContent {
        drawContent()

        state.layoutInfo.visibleItemsInfo.firstOrNull()?.let { firstVisibleItem ->
            if (state.isScrollInProgress || alpha > 0f) {
                // Size of the viewport, the entire size of the scrollable composable we are decorating with
                // this scrollbar.
                val viewportSize =
                    if (horizontal) {
                        size.width
                    } else {
                        size.height
                    } - padding.toPx() * 2

                // The size of the first visible item. We use this to estimate how many items can fit in the
                // viewport. Of course, this works perfectly when all items have the same size. When they
                // don't, the scrollbar knob size will grow and shrink as we scroll.
                val firstItemSize = firstVisibleItem.size

                // The *estimated* size of the entire scrollable composable, as if it's all on screen at
                // once. It is estimated because it's possible that the size of the first visible item does
                // not represent the size of other items. This will cause the scrollbar knob size to grow
                // and shrink as we scroll, if the item sizes are not uniform.
                val estimatedFullListSize = firstItemSize * state.layoutInfo.totalItemsCount

                // The difference in position between the first pixels visible in our viewport as we scroll
                // and the top of the fully-populated scrollable composable, if it were to show all the
                // items at once. At first, the value is 0 since we start all the way to the top (or start
                // edge). As we scroll down (or towards the end), this number will grow.
                val viewportOffsetInFullListSpace =
                    state.firstVisibleItemIndex * firstItemSize + state.firstVisibleItemScrollOffset

                // Where we should render the knob in our composable.
                val knobPosition =
                    (viewportSize / estimatedFullListSize) * viewportOffsetInFullListSpace + padding.toPx()
                // How large should the knob be.
                val knobSize =
                    fixedKnobRatio?.let { it * viewportSize }
                        ?: ((viewportSize * viewportSize) / estimatedFullListSize)

                // Draw the track
                drawRoundRect(
                    color = trackColor,
                    topLeft =
                    when {
                        // When the scrollbar is horizontal and aligned to the bottom:
                        horizontal && alignEnd -> Offset(
                            padding.toPx(),
                            size.height - thickness.toPx()
                        )
                        // When the scrollbar is horizontal and aligned to the top:
                        horizontal && !alignEnd -> Offset(padding.toPx(), 0f)
                        // When the scrollbar is vertical and aligned to the end:
                        alignEnd -> Offset(size.width - thickness.toPx(), padding.toPx())
                        // When the scrollbar is vertical and aligned to the start:
                        else -> Offset(0f, padding.toPx())
                    },
                    size =
                    if (horizontal) {
                        Size(size.width - padding.toPx() * 2, thickness.toPx())
                    } else {
                        Size(thickness.toPx(), size.height - padding.toPx() * 2)
                    },
                    alpha = alpha,
                    cornerRadius = CornerRadius(
                        x = trackCornerRadius.toPx(),
                        y = trackCornerRadius.toPx()
                    ),
                )

                // Draw the knob
                drawRoundRect(
                    color = knobColor,
                    topLeft =
                    when {
                        // When the scrollbar is horizontal and aligned to the bottom:
                        horizontal && alignEnd -> Offset(
                            knobPosition,
                            size.height - thickness.toPx()
                        )
                        // When the scrollbar is horizontal and aligned to the top:
                        horizontal && !alignEnd -> Offset(knobPosition, 0f)
                        // When the scrollbar is vertical and aligned to the end:
                        alignEnd -> Offset(size.width - thickness.toPx(), knobPosition)
                        // When the scrollbar is vertical and aligned to the start:
                        else -> Offset(0f, knobPosition)
                    },
                    size =
                    if (horizontal) {
                        Size(knobSize, thickness.toPx())
                    } else {
                        Size(thickness.toPx(), knobSize)
                    },
                    alpha = alpha,
                    cornerRadius = CornerRadius(
                        x = knobCornerRadius.toPx(),
                        y = knobCornerRadius.toPx()
                    ),
                )
            }
        }
    }
}

//Create PopUp Menu used in the pharmacy panel to add or reduce the quantity of a medicine
//Created also to the flag pharmacy menu

@Composable
fun PopupBox(
    popupWidth: Float,
    popupHeight: Float,
    showPopup: Boolean?,
    content: @Composable () -> Unit
) {


    if (showPopup!!) {
        // full screen background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent.copy(alpha = 0.5f))
                .zIndex(10F),
            contentAlignment = Alignment.Center,
        ) {
            // popup
            Popup(
                alignment = Alignment.Center,
                properties = PopupProperties(
                    excludeFromSystemGesture = true,
                    focusable = true,
                ),

                // to dismiss on click outside
                onDismissRequest = { },
            ) {
                Box(
                    Modifier
                        .width(popupWidth.dp)
                        .height(popupHeight.dp)
                        .background(Color.Gray, RoundedCornerShape(15.dp))
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }

            }
        }
    }
}





