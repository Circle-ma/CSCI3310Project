package com.example.csci3310project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeView(authController: AuthController, onNavigateToTrip: () -> Unit) {
    authController.user?.let { user ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome ${user.name}!", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { authController.logout() }) {
                Text("Logout")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToTrip) {
                Text("Plan a Trip")
            }
        }
    }
}
