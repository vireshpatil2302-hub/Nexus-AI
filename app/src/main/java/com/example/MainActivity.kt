package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        setContent {
            var isDarkTheme by remember { mutableStateOf(sharedPrefs.getBoolean("dark_theme", false)) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val authViewModel: AuthViewModel = viewModel()
                val currentUser by authViewModel.currentUser.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    if (currentUser == null) {
                        AuthScreen(
                            viewModel = authViewModel,
                            modifier = Modifier.padding(innerPadding),
                            darkTheme = isDarkTheme,
                            onToggleDarkTheme = {
                                isDarkTheme = !isDarkTheme
                                sharedPrefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                            }
                        )
                    } else {
                        val chatViewModel: ChatViewModel = viewModel()
                        ChatScreen(
                            viewModel = chatViewModel,
                            modifier = Modifier.padding(innerPadding),
                            onLogout = { authViewModel.logout() },
                            darkTheme = isDarkTheme,
                            onToggleDarkTheme = {
                                isDarkTheme = !isDarkTheme
                                sharedPrefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
