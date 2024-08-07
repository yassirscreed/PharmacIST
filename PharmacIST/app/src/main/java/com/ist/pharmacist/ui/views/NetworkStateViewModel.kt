@file:Suppress("DEPRECATION")

package com.ist.pharmacist.ui.views

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkStateViewModel : ViewModel() {

    private val online = MutableStateFlow(false)
    
    private val _isMeteredConnection = MutableStateFlow(false)
    val isMeteredConnection: StateFlow<Boolean> = _isMeteredConnection.asStateFlow()


    fun updateNetworkState(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isMeteredConnection.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo : NetworkInfo? = connectivityManager.activeNetworkInfo
        online.value = networkInfo?.isConnectedOrConnecting == true
        return online.value
    }
}