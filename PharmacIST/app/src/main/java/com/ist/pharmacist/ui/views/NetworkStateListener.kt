package com.ist.pharmacist.ui.views

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

class NetworkStateListener(private val context: Context, private val viewModel: NetworkStateViewModel) :
    ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        viewModel.updateNetworkState(context)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        viewModel.updateNetworkState(context)
    }
}