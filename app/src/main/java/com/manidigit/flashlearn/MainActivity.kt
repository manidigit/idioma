package com.manidigit.flashlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.manidigit.flashlearn.ui.screens.AppRoot
import com.manidigit.flashlearn.ui.theme.FlashLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FlashLearnTheme { AppRoot() } }
    }
}
