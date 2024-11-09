package com.example.randomizer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.randomizer.ui.theme.RandomizerTheme

class MainActivity : ComponentActivity() {
    lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RandomizerTheme {
                AppNavGraph()
            }
        }

        // Initialize the speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    // Function to start speech recognition
    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        speechRecognizer.startListening(intent)
    }

    // Function to take a photo
    fun takePhoto(context: Context) {
        Toast.makeText(context, "Taking photo!", Toast.LENGTH_SHORT).show()
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        context.startActivity(cameraIntent)
    }

    override fun onStop() {
        super.onStop()
        speechRecognizer.stopListening()
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("next") { NextScreen() }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    Toast.makeText(
                        navController.context,
                        "Navigating to Next Screen!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("next")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Tap anywhere to proceed")
        }
    }
}

@Composable
fun NextScreen() {
    val context = LocalContext.current
    val activity = context as MainActivity

    // State variables to track success
    val scanSuccess = remember { mutableStateOf(false) }
    val photoTaken = remember { mutableStateOf(false) }

    // Set up the speech recognition listener using a LaunchedEffect to start listening when the composable is launched
    LaunchedEffect(Unit) {
        activity.speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.contains("scan")) {
                    scanSuccess.value = true
                    activity.takePhoto(context) // Trigger photo after recognizing "scan"
                    photoTaken.value = true
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        activity.startListening()
    }

    // Show UI for NextScreen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Display a message based on the result
        if (scanSuccess.value && photoTaken.value) {
            Text(text = "Success! Heard 'scan' and took the photo!")
        } else if (scanSuccess.value) {
            Text(text = "Heard 'scan', but no photo was taken.")
        } else if (photoTaken.value) {
            Text(text = "Photo taken, but 'scan' was not heard.")
        } else {
            Text(text = "Say 'scan' to take a photo.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RandomizerTheme {
        MainScreen(navController = rememberNavController())
    }
}
