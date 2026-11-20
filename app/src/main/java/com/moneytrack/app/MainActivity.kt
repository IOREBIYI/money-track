package com.moneytrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.moneytrack.app.auth.AuthViewModel
import com.moneytrack.app.ui.AppRoot
import com.moneytrack.app.ui.AuthGate
import com.moneytrack.app.ui.MoneyTheme

class MainActivity : ComponentActivity() {
    private val vm: MoneyViewModel by viewModels()
    private val auth: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyTheme {
                AuthGate(auth) { AppRoot(vm, auth) }
            }
        }
    }
}
