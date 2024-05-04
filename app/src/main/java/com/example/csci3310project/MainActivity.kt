package com.example.csci3310project

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.csci3310project.ui.theme.CSCI3310ProjectTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val userName = remember { mutableStateOf("Guest") }  // Default to "Guest"
    CSCI3310ProjectTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            UserInterface(innerPadding, userName.value) { name -> userName.value = name }
        }
    }
}

@Composable
fun UserInterface(innerPadding: PaddingValues, userName: String, updateUserName: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Greeting(userName)
        GoogleSignInButton(updateUserName)
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun Map(){
    val singapore = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = MarkerState(position = singapore),
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}

@Composable
fun GoogleSignInButton(updateUserName: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Button(onClick = { googleSignIn(context, coroutineScope, updateUserName) }) {
        Text(text = "Sign in with Google")
    }
}

fun googleSignIn(
    context: Context, coroutineScope: CoroutineScope, updateUserName: (String) -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    val nonce = generateNonce()

    val request = GetCredentialRequest.Builder().addCredentialOption(
        GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(true)
            .setServerClientId("457810545963-uhsk9rjdhekir3q55af5b6j0hqsviv0b.apps.googleusercontent.com")
            .setNonce(nonce).build()
    ).build()

    coroutineScope.launch {
        handleCredentialRequest(context, credentialManager, request, updateUserName)
    }
}

fun generateNonce(): String {
    return UUID.randomUUID().toString().let { uuid ->
        MessageDigest.getInstance("SHA-256").digest(uuid.toByteArray()).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
}

suspend fun handleCredentialRequest(
    context: Context,
    credentialManager: CredentialManager,
    request: GetCredentialRequest,
    updateUserName: (String) -> Unit
) {
    try {
        val result = credentialManager.getCredential(context, request)
        val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        firebaseAuthWithGoogle(googleIdToken, context, updateUserName)
    } catch (e: Exception) {
        Toast.makeText(context, "Authentication error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun firebaseAuthWithGoogle(idToken: String, context: Context, updateUserName: (String) -> Unit) {
    FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().currentUser?.displayName?.let(updateUserName)
                Toast.makeText(context, "Login successful.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Firebase Authentication failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
}