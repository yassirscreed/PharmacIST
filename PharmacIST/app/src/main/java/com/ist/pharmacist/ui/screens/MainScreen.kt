package com.ist.pharmacist.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties
import com.ist.pharmacist.R
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.views.FocusAddressViewModel
import com.ist.pharmacist.ui.views.MarkerLocationsViewModel
import com.ist.pharmacist.ui.views.NetworkStateViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.utils.checkForPermission
import com.ist.pharmacist.utils.checkNearbyPharmaciesAndNotify
import com.ist.pharmacist.utils.getCurrentLocation
import com.ist.pharmacist.utils.notifyUserWithMedicines
import kotlinx.coroutines.launch

/*
Main screen of the app where the user can see the map and interact with the app
and receive notifications
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context,
    username: String?,
    onLogout: () -> Unit = {},
    onAddPharmacy: () -> Unit = {},
    markerLocationsViewModel: MarkerLocationsViewModel,
    onPharmacyPanel: (String) -> Unit = {},
    pharmaciesViewModel: PharmaciesViewModel,
    onShowMoreResults: () -> Unit = {},
    onSearchMedicine: () -> Unit = {},
    focusAddressViewModel: FocusAddressViewModel,
    networkStateViewModel: NetworkStateViewModel,
    user: User,
) {


    val isGuestUser = (user.username == "Guest")

    // Get pharmacies and medicines from view model
    val pharmacyList = pharmaciesViewModel.getPharmacies()

    val medicineList = pharmaciesViewModel.getMedicines()

    // Get favourite pharmacies from view model
    val favouritePharmacies = pharmaciesViewModel.getFavouritePharmaciesFromViewModel(user)



    var showMap by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    val mapProperties by remember { mutableStateOf(MapProperties()) }

    val coroutineScope = rememberCoroutineScope()

    // Check for location permission
    val hasLocationPermission by remember { mutableStateOf(checkForPermission(context)) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            coroutineScope.launch {
                getCurrentLocation(context)?.let {
                    location = it
                    showMap = true
                }
            }
        }
    }

    // Check for notification permission
    var hasNotificationPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Notification permission granted
            hasNotificationPermission = true
        } else {
            // Notification permission denied
            Log.d("Permission", "Notification Permission Denied")
        }
    }
    Log.d("Permission", "hasNotificationPermission: $hasNotificationPermission")

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Notification permission already granted
                hasNotificationPermission = true
            }
        } else {
            // Notification permission not required for Android 12 and below
            hasNotificationPermission = true
        }
    }

    // Observe pharmacy list to notify user of nearby pharmacies
    LaunchedEffect(showMap) {
        /* Update the view model to remove flagged pharmacies of the user and
        globally flagged pharmacies from the map (reassurance)*/
        pharmaciesViewModel.removePharmaciesFlaggedFromMap(user)
        pharmaciesViewModel.removePharmaciesFlaggedGloballyFromMap()
        if (hasNotificationPermission) {
                checkNearbyPharmaciesAndNotify(context, location, pharmacyList)
        }


    }

    Log.d("MainScreen", "hasLocationPermission: $hasLocationPermission")

    // Observe medicine list to notify user of medicines available on favourite pharmacies
    LaunchedEffect(favouritePharmacies) {
        medicineList.observeForever { _ ->
            if (hasNotificationPermission) {
                notifyUserWithMedicines(context, user, medicineList, pharmacyList)
            }
        }
        medicineList.removeObserver { _ -> }
    }

    // Add pharmacies to view model
    addLocationsToViewModelFromPharmacies(pharmacyList, markerLocationsViewModel)



   if (showMap) {

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet(
                    // the actual drawer UI
                    modifier = Modifier.requiredWidth(255.dp), // width does not occupy whole screen
                    drawerContainerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.login_image),
                                contentDescription = "Localized description",
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Space between image and text
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    Image(
                        painter = painterResource(id = R.drawable.user_icon),
                        contentDescription = "User Icon",
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally),

                        colorFilter = ColorFilter.tint(Color.White)
                    )



                    Text(
                        text = username ?: "User",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(200.dp))

                    Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        TextButton(onClick = {
                            /* Pharmacy Click */
                            //Verify if theres wifi or mobile data connection
                            if (networkStateViewModel.isOnline(context) && !isGuestUser) {
                                onAddPharmacy()
                            }
                            //Verify if user is a guest
                            else if (isGuestUser) {
                                Toast.makeText(context, "Guest users cannot add pharmacies", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                            }

                        }) {
                            Text(
                                text = stringResource(id = R.string.add_pharmacy),
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(60.dp)) // Space between buttons
                        TextButton(onClick = {
                            /*Logout Click*/
                            onLogout()
                        }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ExitToApp,
                                    tint = colorResource(id = R.color.black),
                                    contentDescription = "Localized description"
                                )
                                Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
                                Text(
                                    text = stringResource(id = R.string.logout),
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }


                }
            },
        ) {
            Column {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.login_image),
                                contentDescription = "Localized description",
                                modifier = Modifier.size(24.dp) // Adjust size as needed
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Space between image and text
                            Text(
                                stringResource(id = R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },

                    navigationIcon = {


                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                tint = colorResource(id = R.color.black),
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onSearchMedicine() }) {
                            Image(
                                painter = painterResource(id = R.drawable.search_pill), // Replace with your image resource
                                contentDescription = "Localized description",
                                modifier = Modifier.size(40.dp) // Adjust size as needed
                            )
                        }
                    },
                )

                Log.d("FocusAddressViewModel", "FocusAddressViewModel: ${focusAddressViewModel.location.value}")

                MyMap(
                    context = context,
                    latLng = location,
                    mapProperties = mapProperties,
                    onMoveToCurrentLocation = {
                        coroutineScope.launch {
                            getCurrentLocation(context)?.let {
                                location = it
                            }
                        }
                    },
                    onPharmacyPanel = onPharmacyPanel,
                    onShowMoreResults = onShowMoreResults,
                    focusAddressViewModel = focusAddressViewModel,
                    pharmaciesViewModel = pharmaciesViewModel,
                    user = user
                )
            }


        }

    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading...")
        }
    }
}