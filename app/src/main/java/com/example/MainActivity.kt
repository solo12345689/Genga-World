package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.network.LocalMovieBoxServer
import com.example.ui.screens.MovieBoxApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MovieViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MovieViewModel by viewModels()
    private var localServer: LocalMovieBoxServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start local MovieBox proxy server automatically inside the phone when opened
        localServer = LocalMovieBoxServer(applicationContext, 3000)
        localServer?.start()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MovieBoxApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localServer?.stop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.playInfo != null) {
            val pipBuilder = android.app.PictureInPictureParams.Builder()
            val aspectRatio = android.util.Rational(16, 9)
            pipBuilder.setAspectRatio(aspectRatio)
            enterPictureInPictureMode(pipBuilder.build())
        }
    }
}
