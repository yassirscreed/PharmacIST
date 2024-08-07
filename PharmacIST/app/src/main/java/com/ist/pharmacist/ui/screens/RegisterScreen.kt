package com.ist.pharmacist.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ist.pharmacist.R
import com.ist.pharmacist.ui.theme.PharmacISTTheme
import com.ist.pharmacist.ui.views.RegisterViewModel

@Composable
fun RegisterScreen(onLoginClick: () -> Unit, onRegisterSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var usernameAlreadyExists by remember { mutableStateOf(false) }
    var emailAlreadyExists by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }


    val inriaSerifFont = FontFamily(
        Font(R.font.inria_serif_regular),
    )

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.primary)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {

        Image(
            painter = painterResource(id = R.drawable.login_image),
            contentDescription = "Login Image",
            modifier = Modifier
                .padding(10.dp)
                .size(300.dp)
                .clip(CircleShape)
        )

        Text(
            text = stringResource(id = R.string.register),
            fontSize = 30.sp,
            fontFamily = inriaSerifFont,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        EditTextBarField(
            label = stringResource(id = R.string.user_name),
            value = username,
            onValueChange = { username = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            hide = false,
            isError = usernameAlreadyExists,
            supportingText = if (usernameAlreadyExists) stringResource(id = R.string.username_exists) else "",
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )

        EditTextBarField(
            label = stringResource(id = R.string.email),
            value = email,
            onValueChange = { email = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            hide = false,
            isError = emailError || emailAlreadyExists,
            supportingText = if (emailError) stringResource(id = R.string.invalid_email) else if (emailAlreadyExists) stringResource(
                id = R.string.email_exists
            ) else "",
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )

        EditTextBarField(
            label = stringResource(id = R.string.password),
            value = password,
            onValueChange = { password = it  },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            hide = true,
            isError = passwordError,
            supportingText = if (passwordError) stringResource(id = R.string.invalid_password) else "",
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )


        TextButton(
            onClick = { onLoginClick() },
            modifier = Modifier
                .padding(bottom = 10.dp)
        ) {
            Text(text = stringResource(id = R.string.already_have_account), color = MaterialTheme.colorScheme.onPrimary)
        }

        Button(
            onClick = {
                Log.d("Register", "Register button clicked")

                Log.d("Register", "Username: $username")
                //log passworf error
                Log.d("Register", "Email: $email")
                Log.d("Register", "Password ERROR: $passwordError")
                Log.d("Register", "Email ERROR: $emailError")

                // Handle register
                emailError = !isValidEmail(email)
                passwordError = password.length < 6
                if (!emailError && !passwordError) {
                    // Register user
                    RegisterViewModel().registerUser(username, email, password, onSuccess = {
                        Log.d("Register", "User registered successfully")
                        emailAlreadyExists = false
                        usernameAlreadyExists = false
                        onRegisterSuccess()
                    },
                        onEmailExists = {
                            emailAlreadyExists = true
                            usernameAlreadyExists = false
                            Log.d("Register", "Email already exists")
                        },
                        onUsernameExists = {
                            usernameAlreadyExists = true
                            emailAlreadyExists = false
                            Log.d("Register", "Username already exists")
                        },
                        onFail = {
                            Log.d("Register", "User registration failed")
                        })

                }


            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier
                .size(300.dp, 50.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.register_button))
        }


    }
}

fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}


@Preview(showBackground = true)
@Composable
fun RegisterPreview() {
    PharmacISTTheme {
        RegisterScreen(onLoginClick = {}, onRegisterSuccess = {})
    }
}
