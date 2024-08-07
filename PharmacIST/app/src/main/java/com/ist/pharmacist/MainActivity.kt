package com.ist.pharmacist

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.google.android.libraries.places.api.Places
import com.ist.pharmacist.database.MedicineDatabase
import com.ist.pharmacist.database.MedicineRepository
import com.ist.pharmacist.database.PharmacyDatabase
import com.ist.pharmacist.database.PharmacyRepository
import com.ist.pharmacist.ui.theme.PharmacISTTheme
import com.ist.pharmacist.ui.views.NetworkStateListener
import com.ist.pharmacist.ui.views.NetworkStateViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import com.ist.pharmacist.ui.views.PharmaciesViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var networkStateListener: NetworkStateListener
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkStateViewModel: NetworkStateViewModel
    private lateinit var pharmaciesViewModel: PharmaciesViewModel

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Places SDK
        Places.initialize(applicationContext, getString(R.string.google_api))

        // Network State
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkStateViewModel = NetworkStateViewModel()

        // Initialize the Room databases
        val medicineDatabase = Room.databaseBuilder(
            applicationContext,
            MedicineDatabase::class.java,
            "medicine_database"
        ).build()

        val pharmacyDatabase = Room.databaseBuilder(
            applicationContext,
            PharmacyDatabase::class.java,
            "pharmacy_database"
        ).build()

        // Initialize the DAOs
        val medicineDao = medicineDatabase.medicineDao()
        val pharmacyDao = pharmacyDatabase.pharmacyDao()

        // Initialize the repositories
        val medicineRepository = MedicineRepository(medicineDao)
        val pharmacyRepository = PharmacyRepository(pharmacyDao)

        // Initialize the ViewModelFactory
        val pharmaciesViewModelFactory = PharmaciesViewModelFactory(pharmacyRepository, medicineRepository)

        // Create the PharmaciesViewModel
        pharmaciesViewModel = ViewModelProvider(this, pharmaciesViewModelFactory)[PharmaciesViewModel::class.java]



        setContent {
            PharmacISTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pharmaciesViewModel: PharmaciesViewModel = viewModel(factory = pharmaciesViewModelFactory)
                    PharmacyScreen(context = this, networkStateViewModel = networkStateViewModel, pharmaciesViewModel = pharmaciesViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        networkStateListener = NetworkStateListener(this, networkStateViewModel)
        connectivityManager.registerDefaultNetworkCallback(networkStateListener)
    }

    override fun onPause() {
        super.onPause()
        connectivityManager.unregisterNetworkCallback(networkStateListener)
    }

    override fun onStop() {
        super.onStop()
        pharmaciesViewModel.updateRepositories()
    }
}



