package com.example.csci3310project

import android.content.Context
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class AuthController(private val navController: NavController) {
    var user: User? = null
        private set

    fun googleSignIn(context: Context) {
        val nonce = generateNonce()
        val googleIdOption =
            GetSignInWithGoogleOption.Builder("457810545963-uhsk9rjdhekir3q55af5b6j0hqsviv0b.apps.googleusercontent.com")
                .setNonce(nonce).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        CoroutineScope(Dispatchers.Main).launch {
            handleCredentialRequest(context, request)
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        user = null
        navController.navigate("login") {
            popUpTo("home") { inclusive = true }
        }
    }

    private fun generateNonce(): String = UUID.randomUUID().toString().let { uuid ->
        MessageDigest.getInstance("SHA-256").digest(uuid.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private suspend fun handleCredentialRequest(context: Context, request: GetCredentialRequest) {
        val credentialManager = CredentialManager.create(context)
        try {
            val result = credentialManager.getCredential(context, request)
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            firebaseAuthWithGoogle(googleIdToken, context)
        } catch (e: Exception) {
            Toast.makeText(context, "Authentication error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?, context: Context) {
        idToken?.let {
            val credential = GoogleAuthProvider.getCredential(it, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        user = User(firebaseUser?.displayName ?: "Anonymous")
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        Toast.makeText(
                            context, "Firebase Authentication failed.", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}
