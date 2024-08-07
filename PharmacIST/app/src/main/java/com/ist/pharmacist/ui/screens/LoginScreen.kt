package com.ist.pharmacist.ui.screens

import android.annotation.SuppressLint
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
import com.ist.pharmacist.ui.views.LoginViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginScreen(onRegisterClick: () -> Unit, onLoginSuccess: (String) -> Unit = {}) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }


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
            text = stringResource(id = R.string.app_name),
            fontSize = 30.sp,
            fontFamily = inriaSerifFont,
            color = MaterialTheme.colorScheme.onPrimary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        EditTextBarField(
            label = stringResource(id = R.string.user_name),
            value = username,
            onValueChange = { username = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            hide = false,
            isError = loginError,
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )
        EditTextBarField(
            label = stringResource(id = R.string.password),
            value = password,
            onValueChange = { password = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            hide = true,
            isError = loginError,
            supportingText = if (loginError) stringResource(id = R.string.invalid_login) else "",
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )

        TextButton(
            onClick = { onRegisterClick() },
            modifier = Modifier
                .padding(bottom = 10.dp)
        ) {
            Text(text = stringResource(id = R.string.register_request), color = MaterialTheme.colorScheme.onPrimary)
        }

        Button(
            onClick = {
                /* Handle login */
                LoginViewModel().loginUser(username, password, onSuccess = {
                    Log.d("LoginScreen", "Login successful")
                    onLoginSuccess(username)
                    loginError = false
                }, onFail = {
                    Log.d("LoginScreen", "Login failed")
                    loginError = true
                })
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
            Text(text = stringResource(id = R.string.login))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                /* Handle login */
                LoginViewModel().loginUser("Guest", "GuestPassword", onSuccess = {
                    Log.d("LoginScreen", "Login successful")
                    //fix guest login distinction
                    onLoginSuccess("Guest")
                    loginError = false
                }, onFail = {
                    Log.d("LoginScreen", "Login failed")
                    loginError = true
                })
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
            Text(text = stringResource(id = R.string.guest_login))
        }


    }


}


@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    PharmacISTTheme {
        LoginScreen(onRegisterClick = {}, onLoginSuccess = {})
    }
}
