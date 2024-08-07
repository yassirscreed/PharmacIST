package com.ist.pharmacist.ui.views

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ist.pharmacist.data.User
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun registerUser(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onEmailExists: () -> Unit,
        onUsernameExists: () -> Unit,
        onFail: () -> Unit
    ) {

        viewModelScope.launch {
            // Check if email is already taken
            db.collection("Profiles")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { emailSnapshot ->
                    if (!emailSnapshot.isEmpty) {
                        Log.d("RegisterViewModel", "Email already exists")
                        onEmailExists()
                        onFail()
                        return@addOnSuccessListener
                    }

                    // Check if username is already taken
                    db.collection("Profiles")
                        .whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener { usernameSnapshot ->
                            if (!usernameSnapshot.isEmpty) {
                                Log.d("RegisterViewModel", "Username already exists")
                                onUsernameExists()
                                onFail()
                                return@addOnSuccessListener
                            }

                            // If email and username are available, create the user
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val firebaseUser = auth.currentUser

                                        val user = User(username, email, password)

                                        firebaseUser?.let {
                                            db.collection("Profiles")
                                                .document(username)
                                                .set(user)
                                                .addOnSuccessListener {
                                                    onSuccess()
                                                }
                                                .addOnFailureListener { _ ->
                                                    onFail()
                                                }
                                        }
                                    } else {
                                        onFail()
                                    }
                                }
                        }
                }
        }
    }
}