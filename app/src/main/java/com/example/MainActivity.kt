package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppScaffold
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ShopViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: ShopViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIntent(intent)
    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val systemInDark = isSystemInDarkTheme()
      val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
      }

      MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainAppScaffold(viewModel = viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    val target = intent?.getStringExtra("screen")
    if (!target.isNullOrBlank()) {
      navigateToScreen(target)
    }
  }

  /**
   * Navigate to a specific screen tab in the app.
   */
  fun navigateToScreen(screen: ShopScreen) {
    viewModel.navigateToScreen(screen)
  }

  /**
   * Navigate to a screen by name (e.g. "POS", "INVENTORY", "DASHBOARD").
   */
  fun navigateToScreen(screenName: String) {
    viewModel.navigateToScreen(screenName)
  }
}
