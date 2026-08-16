package com.healthos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.healthos.presentation.HealthOsApp
import dagger.hilt.android.AndroidEntryPoint

import com.healthos.presentation.theme.HealthOsTheme
import com.healthos.presentation.theme.MidnightInk

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )
        window.statusBarColor = Color.rgb(2, 7, 23)
        window.navigationBarColor = Color.rgb(2, 7, 23)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
        setContent {
            HealthOsTheme {
                Surface(color = MidnightInk) {
                    HealthOsApp()
                }
            }
        }
    }
}
