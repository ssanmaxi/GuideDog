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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat



// MainActivity is the entry point of the app
class MainActivity : ComponentActivity() {
    // SpeechRecognizer instance to handle speech input
    lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // requests microphone permission for android microphone api
        checkMicrophonePermission()

        setContent {
            // Set the theme and initialize the navigation graph
            RandomizerTheme {
                AppNavGraph()
            }
        }

        // Initialize the speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    // checks and requests permission
    fun checkMicrophonePermission(){
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            // Permission already granted
            // You can initialize components that require the permission here if needed
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "Microphone permission is required for this app", Toast.LENGTH_LONG).show()
                // You might want to disable functionality that depends on this permission
            }
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    // Function to start speech recognition
    fun startListening() {

        // Check if permission is granted and only allows intent if so
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ){
            // Intent to trigger speech recognition
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            speechRecognizer.startListening(intent)
        } else {
            // get permission
            checkMicrophonePermission()
        }

    }

    // Function to take a photo using the device's camera
    fun takePhoto(context: Context) {
        // Show a toast message indicating photo capture
        Toast.makeText(context, "Taking photo!", Toast.LENGTH_SHORT).show()
        // Intent to open the camera app
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        context.startActivity(cameraIntent)
    }

    // Stop listening when the activity is no longer visible
    override fun onStop() {
        super.onStop()
        speechRecognizer.stopListening()
    }
}

// Composable function to set up the navigation graph
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        // Define the composable destinations
        composable("main") { MainScreen(navController) }
        composable("next") { NextScreen() }
    }
}

// Main screen of the app
@Composable
fun MainScreen(navController: NavController) {
    Scaffold {
        // Box that fills the entire screen and is clickable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // On click, show a toast and navigate to the next screen
                    Toast.makeText(
                        navController.context,
                        "Navigating to Next Screen!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("next")
                },
            contentAlignment = Alignment.Center
        ) {
            // Display text in the center of the screen
            Text(text = "Tap anywhere to proceed")
        }
    }
}

// Next screen where the main logic happens
@Composable
fun NextScreen() {
    // Obtain the current context and cast it to MainActivity
    val context = LocalContext.current
    val activity = context as MainActivity

    // State variables to track if "scan" was heard and if the photo was taken
    val scanSuccess = remember { mutableStateOf(false) }
    val photoTaken = remember { mutableStateOf(false) }

    // Set up the speech recognition listener
    LaunchedEffect(Unit) {


        // Ensure the permission is granted before starting to listen
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            activity.startListening()
        } else {
            // Request permission
            activity.checkMicrophonePermission()
        }


        // Initialize the RecognitionListener
        activity.speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                // Obtain the recognized words
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                // Check if "scan" is among the recognized words
                if (matches != null && matches.contains("scan")) {
                    // Update state variables
                    scanSuccess.value = true
                    // Call the function to take a photo
                    activity.takePhoto(context)
                    photoTaken.value = true
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        // Start listening for speech input
        activity.startListening()
    }

    // UI for the NextScreen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Display messages based on the recognition and photo capture results
        when {
            scanSuccess.value && photoTaken.value -> {
                Text(text = "Success! Heard 'scan' and took the photo!")
            }
            scanSuccess.value -> {
                Text(text = "Heard 'scan', but no photo was taken.")
            }
            photoTaken.value -> {
                Text(text = "Photo taken, but 'scan' was not heard.")
            }
            else -> {
                Text(text = "Say 'scan' to take a photo.")
            }
        }
    }
}

// Preview function for the MainScreen
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RandomizerTheme {
        MainScreen(navController = rememberNavController())
    }
}
