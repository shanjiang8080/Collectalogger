package com.example.collectalogger2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.collectalogger2.navigation.CollectaloggerNavGraph
import com.example.collectalogger2.ui.theme.Collectalogger2Theme
import com.example.collectalogger2.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Collectalogger2Theme {
                CollectaloggerNavGraph()
            }
        }
    }
}

fun CreationExtras.collectaloggerApplication(): CollectaloggerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as CollectaloggerApplication)