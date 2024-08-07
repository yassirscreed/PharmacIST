package com.ist.pharmacist

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ist.pharmacist.data.User
import com.ist.pharmacist.ui.screens.AddLocationScreen
import com.ist.pharmacist.ui.screens.AddPharmacyScreen
import com.ist.pharmacist.ui.screens.AddressSearchBarScreen
import com.ist.pharmacist.ui.screens.CreateMedicineScreen
import com.ist.pharmacist.ui.screens.LocationPermissionScreen
import com.ist.pharmacist.ui.screens.LoginScreen
import com.ist.pharmacist.ui.screens.MainScreen
import com.ist.pharmacist.ui.screens.MedicinePanelScreen
import com.ist.pharmacist.ui.screens.MedicineSearchBarScreen
import com.ist.pharmacist.ui.screens.PharmacyPanelScreen
import com.ist.pharmacist.ui.screens.RegisterScreen
import com.ist.pharmacist.ui.screens.SuspendScreen
import com.ist.pharmacist.ui.views.AddMedicineViewModel
import com.ist.pharmacist.ui.views.AddPharmacyViewModel
import com.ist.pharmacist.ui.views.FocusAddressViewModel
import com.ist.pharmacist.ui.views.MarkerLocationsViewModel
import com.ist.pharmacist.ui.views.NetworkStateViewModel
import com.ist.pharmacist.ui.views.NewMarkerLocationViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.ui.views.PhotoCacheViewModel
import com.ist.pharmacist.ui.views.UserViewModel
import com.ist.pharmacist.utils.checkForPermission
import com.ist.pharmacist.utils.drawTrack
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {


    data object LoginScreen : Screen("loginScreen")
    data object RegisterScreen : Screen("registerScreen")
    data object MainScreen : Screen("mainScreen")
    data object AddPharmacyMenu : Screen("addPharmacyScreen")
    data object AddressSearchMenu : Screen("addressSearchScreen")
    data object MedicineSearchMenu : Screen("medicineSearchScreen")

    data object AddLocationMenu : Screen("addLocationScreen")
    data object LocationPermissionScreen : Screen("locationPermissionScreen")
    data object PharmacyPanelScreen : Screen("pharmacyPanelScreen")

    data object MedicinePanelScreen : Screen("medicinePanelScreen")

    data object CreateMedicineScreen : Screen("createMedicineScreen")

    data object SuspendedScreen : Screen("suspendedScreen")

}

@Composable
fun PharmacyScreen(
    context: Context,
    networkStateViewModel: NetworkStateViewModel,
    pharmaciesViewModel: PharmaciesViewModel
) {
    val navController = rememberNavController()


    // Create the ViewModel here

    // Create the ViewModel here
    val markerLocationsViewModel: MarkerLocationsViewModel = viewModel()

    //New Marker Location ViewModel
    val newMarkerLocationViewModel: NewMarkerLocationViewModel = viewModel()

    // Add Pharmacy ViewModel
    val addPharmacyViewModel: AddPharmacyViewModel = viewModel()


    // Add Medicine ViewModel
    val addMedicineViewModel: AddMedicineViewModel = viewModel()

    // Focus Address ViewModel
    val focusAddressViewModel: FocusAddressViewModel = viewModel()

    // User ViewModel
    val userViewModel: UserViewModel = viewModel()

    // Photo Cache ViewModel
    val photoCacheViewModel: PhotoCacheViewModel = viewModel()

    val coroutineScope = rememberCoroutineScope()

    // Observe network state changes
    val isMeteredConnection by networkStateViewModel.isMeteredConnection.collectAsState()

    LaunchedEffect(isMeteredConnection) {
        // Refresh the screen when network state changes
        // You can perform any necessary actions here, such as refetching data or updating UI

    }

    //Begging of the navigation
    NavHost(navController = navController, startDestination = Screen.LoginScreen.route) {



        //Login navigation handler
        composable(Screen.LoginScreen.route) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Screen.RegisterScreen.route) },
                onLoginSuccess = { username ->
                    coroutineScope.launch {
                        val user: User? = if (username == "Guest") {
                            User("Guest", "", "")
                        } else {
                            pharmaciesViewModel.getUserByUsername(username)
                        }
                        Log.d("LoginScreen", "User: $user")
                        user?.let {
                            userViewModel.setUser(it)
                            if (checkForPermission(context)) {
                                pharmaciesViewModel.updateViewModel(context, user)
                                /* Update the view model to remove flagged pharmacies of the user and
                                globally flagged pharmacies from the map (reassurance)*/
                                pharmaciesViewModel.removePharmaciesFlaggedFromMap(user)
                                pharmaciesViewModel.removePharmaciesFlaggedGloballyFromMap()
                                // Update the view model with the user's data and check if the user is suspended
                                pharmaciesViewModel.suspendUserIfNeeded(user)
                                if (pharmaciesViewModel.isUserSuspended(user)){
                                    navController.navigate(Screen.SuspendedScreen.route)
                                }
                                else {
                                    navController.navigate(Screen.MainScreen.route) {
                                        popUpTo(Screen.LoginScreen.route) { inclusive = true }
                                    }
                                }
                            } else {
                                navController.navigate(Screen.LocationPermissionScreen.route)
                            }
                        }
                    }
                }
            )
        }

        //Register navigation handler
        composable(Screen.RegisterScreen.route) {
            RegisterScreen(
                onLoginClick = { navController.navigate(Screen.LoginScreen.route) },
                onRegisterSuccess = { navController.navigate(Screen.LoginScreen.route) }
            )
        }

        //Main navigation handler
        composable(Screen.MainScreen.route) {
            val user = userViewModel.user.value
            if (user != null) {
                // Update the view model with the user's data and check if the user is suspended
                pharmaciesViewModel.suspendUserIfNeeded(user)
                /* Update the view model to remove flagged pharmacies of the user and
                globally flagged pharmacies from the map (reassurance)*/
                pharmaciesViewModel.removePharmaciesFlaggedFromMap(user)
                pharmaciesViewModel.removePharmaciesFlaggedGloballyFromMap()
                if(pharmaciesViewModel.isUserSuspended(user)){
                    navController.navigate(Screen.SuspendedScreen.route)
                }
                else {
                    MainScreen(
                        context = context,
                        username = user.username,
                        onAddPharmacy = { navController.navigate(Screen.AddPharmacyMenu.route) },
                        onLogout = {
                            userViewModel.setUser(User("", "", ""))
                            navController.navigate(Screen.LoginScreen.route)

                        },
                        markerLocationsViewModel = markerLocationsViewModel,
                        onPharmacyPanel = { pharmacyID ->
                            navController.navigate(
                                Screen.PharmacyPanelScreen.route
                                        + "?pharmacyID=$pharmacyID"
                            )
                        },
                        pharmaciesViewModel = pharmaciesViewModel,
                        onShowMoreResults = { navController.navigate(Screen.AddressSearchMenu.route) },
                        onSearchMedicine = { navController.navigate(Screen.MedicineSearchMenu.route) },
                        focusAddressViewModel = focusAddressViewModel,
                        user = user,
                        networkStateViewModel = networkStateViewModel
                    )
                }
            }
        }

        //Location permission navigation handler
        composable(Screen.LocationPermissionScreen.route) {
            LocationPermissionScreen(
                onPermissionGranted = {
                    val user = userViewModel.user.value
                    if (user != null) {
                        pharmaciesViewModel.updateViewModel(context, user)
                        pharmaciesViewModel.suspendUserIfNeeded(user)
                        navController.navigate(Screen.MainScreen.route) {
                            popUpTo(Screen.LoginScreen.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        //Add pharmacy navigation handler
        composable(Screen.AddPharmacyMenu.route) {
            AddPharmacyScreen(
                context = context,
                backHandler = { navController.popBackStack() },
                addLocationHandler = { navController.navigate(Screen.AddLocationMenu.route) },
                goToMainMenu = { navController.navigate(Screen.MainScreen.route) },
                markerLocationsViewModel = markerLocationsViewModel,
                newMarkerLocation = newMarkerLocationViewModel,
                addPharmacyViewModel = addPharmacyViewModel,
                pharmaciesViewModel = pharmaciesViewModel,
                user = userViewModel.user.value!!,
            )
        }

        //Address search navigation handler
        composable(Screen.AddressSearchMenu.route) {
            AddressSearchBarScreen(
                backHandler = { navController.popBackStack() },
                context = context,
                navigateToMapScreen = { address ->
                    focusAddressViewModel.location.value = address
                    navController.popBackStack()
                }

            )
        }

        //Medicine search navigation handler
        composable(Screen.MedicineSearchMenu.route) {
            MedicineSearchBarScreen(
                backHandler = { navController.popBackStack() },
                context = context,
                viewModel = pharmaciesViewModel,
                onPharmacyPanel = { pharmacyID ->
                    navController.navigate(
                        Screen.PharmacyPanelScreen.route
                                + "?pharmacyID=$pharmacyID"
                    )
                }
            )
        }

        //Add location navigation handler
        composable(Screen.AddLocationMenu.route) {
            AddLocationScreen(
                context = context,
                backHandler = { navController.popBackStack() },
                navigateToAddPharmacy = { newLocation ->
                    newMarkerLocationViewModel.location.value = newLocation
                    navController.popBackStack()
                },
                markerLocationsViewModel = markerLocationsViewModel,
                focusAddressViewModel = focusAddressViewModel
            )
        }

        //Pharmacy panel navigation handler
        composable(Screen.PharmacyPanelScreen.route + "?pharmacyID={pharmacyID}") { backStackEntry ->
            val pharmacyName = backStackEntry.arguments?.getString("pharmacyID")
            val user = userViewModel.user.value
            if (pharmacyName != null && user != null) {
                PharmacyPanelScreen(
                    context = context,
                    backHandler = {
                        navController.popBackStack()
                    },
                    pharmaciesViewModel = pharmaciesViewModel,
                    photoCacheViewModel = photoCacheViewModel,
                    pharmacyID = pharmacyName,
                    onCreateMedicineClick = { pharmacyID ->
                        navController.navigate(Screen.CreateMedicineScreen.route + "?pharmacyID=$pharmacyID")
                    },
                    onGoThereClick = { latLng ->
                        if (latLng != null) {
                            Log.d("PharmacyPanelScreen", "Go there: $latLng")
                            drawTrack(latLng.latitude, latLng.longitude, context)
                        } else {
                            // Handle the case when place details fetching fails
                            Log.e(
                                "PharmacyPanelScreen",
                                "Failed to fetch place details for pharmacy: $pharmacyName"
                            )
                        }
                    },
                    onMedicineClick = { barcode ->
                        navController.navigate(Screen.MedicinePanelScreen.route + "?barcode=$barcode")
                    },
                    user = user,
                    networkStateViewModel = networkStateViewModel,
                    onFavoriteClick = { pharmacyId ->
                        pharmaciesViewModel.toggleFavoritePharmacy(user, pharmacyId)
                    },
                    onFlagClick = { pharmacyId ->
                        pharmaciesViewModel.toggleFlaggedPharmacy(user, pharmacyId)
                        navController.navigate(Screen.MainScreen.route)
                    }
                )
            }
        }

        //Create medicine navigation handler
        composable(Screen.CreateMedicineScreen.route + "?pharmacyID={pharmacyID}") { backStackEntry ->
            val pharmacyID = backStackEntry.arguments?.getString("pharmacyID")
            if (pharmacyID != null) {
                CreateMedicineScreen(
                    context = context,
                    backHandler = {
                        navController.popBackStack()
                    },
                    pharmaciesViewModel = pharmaciesViewModel,
                    pharmacyID = pharmacyID,
                    addMedicineViewModel = addMedicineViewModel
                )
            }
        }

        //Medicine panel navigation handler
        composable(Screen.MedicinePanelScreen.route + "?barcode={barcode}") { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")
            val user = userViewModel.user.value
            val medicine = barcode?.let { pharmaciesViewModel.getMedicineByBarcode(it) }
            if (barcode != null && user != null) {
                MedicinePanelScreen(
                    context = context,
                    backHandler = {
                        navController.popBackStack()
                    },
                    pharmaciesViewModel = pharmaciesViewModel,
                    photoCacheViewModel = photoCacheViewModel,
                    networkStateViewModel = networkStateViewModel,
                    barcode = barcode,
                    user = user,
                    onPharmacyButtonClick = { pharmacyID ->
                        navController.navigate(Screen.PharmacyPanelScreen.route + "?pharmacyID=$pharmacyID")
                    },
                    onNotifyUser = {
                        if (medicine != null) {
                            pharmaciesViewModel.toggleUserToNotify(medicine, user.username)
                        }
                    }
                )
            }
        }

        composable(Screen.SuspendedScreen.route) {
            SuspendScreen (
                onLogout = {
                    userViewModel.setUser(User("", "", ""))
                    navController.navigate(Screen.LoginScreen.route)
                }
            )
        }
    }
}
