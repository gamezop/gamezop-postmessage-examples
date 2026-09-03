package com.gamezop.postmessageexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamezop.postmessageexample.data.GameEventViewModel
import com.gamezop.postmessageexample.ui.GamezopPostMessageApp
import com.gamezop.postmessageexample.ui.theme.GamezopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GamezopTheme {
                val eventViewModel: GameEventViewModel = viewModel()
                GamezopPostMessageApp(eventViewModel)
            }
        }
    }
}

