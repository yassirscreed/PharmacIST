package com.ist.pharmacist.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

// Class to scan barcodes
class BarcodeScanner(
    private val applicationContext: Context,
) {

    // Options for barcode scanner
    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()

    private lateinit var scanner: GmsBarcodeScanner

    val barCodeResult = MutableStateFlow<String?>(null)

    init {
        installBarcodeScanner()
    }

    // Install barcode scanner
    private fun installBarcodeScanner() {
        val moduleInstall = ModuleInstall.getClient(applicationContext)
        val moduleInstallRequest = ModuleInstallRequest.newBuilder()
            .addApi(GmsBarcodeScanning.getClient(applicationContext))
            .build()
        moduleInstall
            .installModules(moduleInstallRequest)
            .addOnSuccessListener {
                scanner = if (it.areModulesAlreadyInstalled()) {
                    // Modules are already installed when the request is sent.
                    GmsBarcodeScanning.getClient(applicationContext, options)
                } else {
                    // Modules are installed after the request is sent.
                    GmsBarcodeScanning.getClient(applicationContext, options)
                }
            }
            .addOnFailureListener {
                // Handle failure…
                Log.e("BarcodeScanner", "Error installing barcode scanning module", it)
            }
    }

    // Scan barcode
    suspend fun scanBarcode(param: (String?) -> Unit) {
        try {
            val result = scanner.startScan().await()
            val barcodeValue = result.rawValue
            barCodeResult.value = barcodeValue
            Log.d("BarcodeScanner", "Barcode scanned: $barcodeValue")
            param(barcodeValue)
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Error scanning barcode", e)
            param(null)
        }
    }
}