package com.ist.pharmacist.ui.views

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoginViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    fun loginUser(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ): Boolean {
        var loginSuccessful = false

        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Attempting to log in user"
                        + " with username: $username and password: $password")
                val querySnapshot = firestore.collection("Profiles")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password)
                    .get()
                    .await()

                Log.d("LoginViewModel", "Query snapshot: $querySnapshot")


                if (!querySnapshot.isEmpty) {
                    Log.d("LoginViewModel", "User logged in successfully")
                    loginSuccessful = true
                    // User logged in successfully
                    onSuccess()
                } else {
                    Log.d("LoginViewModel", "Invalid username or password")
                    // Invalid username or password
                    loginSuccessful = false
                    onFail()
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error occurred during login", e)
                // Error occurred during login
                loginSuccessful = false
            }
        }

        return loginSuccessful
    }
}