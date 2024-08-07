package com.ist.pharmacist.ui.screens

//import androidx.compose.material.icons.filled.History
//import androidx.compose.material.icons.filled.Medication
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.ist.pharmacist.R
import com.ist.pharmacist.utils.getLatLngFromAddress

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AddressSearchBarScreen(backHandler: () -> Unit,
                           context: Context,
                           navigateToMapScreen: (LatLng) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val placesClient = Places.createClient(context)
    var addressSelected by remember { mutableStateOf(false) }
    var addressList by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }


    LaunchedEffect(addressSelected) {
        if (addressSelected) {
            addressList = emptyList()
            text = ""
            searchActive = false
            addressSelected = false
        }
    }

    Scaffold (
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Spacer(modifier = Modifier.width(8.dp)) // Space between image and text
                        Text(
                            stringResource(id = R.string.search_address),
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
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                //Search bar to search for addresses handler
                androidx.compose.material3.SearchBar(query = text,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp, 0.dp, 15.dp, 15.dp),
                    onQueryChange = {
                        text = it
                        searchActive = true

                        // Create a token for the autocomplete session
                        val token = AutocompleteSessionToken.newInstance()

                        // create a request for autocomplete predictions
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setCountries("PT") // restricted to Portugal
                            .setTypesFilter(listOf(PlaceTypes.ADDRESS)) // restrict to addresses
                            .setSessionToken(token)
                            .setQuery(it)
                            .build()

                        // AutoComplete request
                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                addressList = response.autocompletePredictions.take(10)
                            }
                            .addOnFailureListener { exception ->
                                if (exception is ApiException) {
                                    Log.e("Places", "Place not found: " + exception.statusCode)
                                }
                            }
                    },
                    onSearch = {
                        searchActive = false
                    },
                    active = searchActive,
                    onActiveChange = {
                        searchActive = it
                    },
                    placeholder = {
                        Text(text = "Search address...")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if (searchActive) {
                            Icon(
                                modifier = Modifier.clickable {
                                    if (text.isEmpty()) {
                                        searchActive = false
                                    } else {
                                        text = ""
                                    }
                                },
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Icon"
                            )
                        }

                    }
                )
                {
                    // Display the address list if the search is active
                    addressList.forEach {
                        if (it.getFullText(null).toString().contains(text, ignoreCase = true)) {
                            Row(modifier = Modifier.padding(all = 2.dp)) {
                                Button(
                                    onClick = {
                                              Log.d("AddressSearchBarScreen", "Address selected: ${it.getFullText(null)}")
                                              val addressLatLng = getLatLngFromAddress(context, it.getFullText(null).toString())
                                              if (addressLatLng != null) {
                                                  navigateToMapScreen(addressLatLng)
                                              }

                                    }, //nathaniel
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 0.dp)
                                        .height(60.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                ){
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = "Medication Icon",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .offset(x = (-8).dp)
                                    )
                                    Text(text = it.getFullText(null).toString(),
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f))

                                }
                            }
                        }
                    }
                }

            }


        }
    )

}