package com.example.randomizer

import android.util.Log
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.randomizer.ui.theme.RandomizerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    // SpeechRecognizer instance to handle speech input
    lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions
        checkMicrophonePermission()
        checkCameraPermission()

        setContent {
            // Set the theme and initialize the navigation graph
            RandomizerTheme {
                AppNavGraph()
            }
        }

        // Initialize the speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    // Function to check and request microphone permission
    fun checkMicrophonePermission() {
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
        }
    }

    // Function to check and request camera permission
    fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val REQUEST_CAMERA_PERMISSION = 201
    }

    // Handle the result of the permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Microphone permission granted
                    Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Microphone permission denied
                    Toast.makeText(
                        this,
                        "Microphone permission is required for this app",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Camera permission granted
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Camera permission denied
                    Toast.makeText(
                        this,
                        "Camera permission is required to take photos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Function to start speech recognition
    fun startListening() {
        // Check if permission is granted and only allow intent if so
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Intent to trigger speech recognition
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            speechRecognizer.startListening(intent)
        } else {
            // Request permission
            checkMicrophonePermission()
        }
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

    // State variables to track if "scan" was heard and the captured image
    val scanSuccess = remember { mutableStateOf(false) }
    val capturedImage = remember { mutableStateOf<Bitmap?>(null) }

    // Temporary URI to store the image
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Launcher to take a picture and get the result
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {

            // Load the image from the URI and update the state
            imageUri.value?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                capturedImage.value = bitmap
            }
        }
    }

    // Set up the speech recognition listener
    LaunchedEffect(Unit) {
        // Ensure the microphone permission is granted before starting to listen
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
                    val matches =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    // Check if "scan" is among the recognized words
                    if (matches != null && matches.contains("scan")) {
                        // Update state variables
                        scanSuccess.value = true
                        // Prepare to take a photo
                        val photoFile = createImageFile(context)
                        imageUri.value = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            photoFile
                        )
                        // Before launching the camera, check camera permission
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            // Launch the camera to take a picture
                            takePictureLauncher.launch(imageUri.value)
                        } else {
                            // Request camera permission
                            activity.checkCameraPermission()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            // Start listening for speech input
            activity.startListening()
        } else {
            // Request microphone permission
            activity.checkMicrophonePermission()
        }
    }

    // UI for the NextScreen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (capturedImage.value != null) {
            // Display the captured image
            Image(
                bitmap = capturedImage.value!!.asImageBitmap(),
                contentDescription = "Captured Image"
            )
        } else {
            Text(text = "Say 'scan' to take a photo.")
        }
    }
}

// Function to create a temporary image file
fun createImageFile(context: Context): File {
    val timestamp = System.currentTimeMillis()
    val storageDir = context.cacheDir
    return File.createTempFile(
        "JPEG_${timestamp}_",
        ".jpg",
        storageDir
    )
}

// Preview function for the MainScreen
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RandomizerTheme {
        MainScreen(navController = rememberNavController())
    }
}
