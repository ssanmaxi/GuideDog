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
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.os.Build
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var textToSpeech: TextToSpeech
    var isTtsInitialized by mutableStateOf(false)  // Track TTS initialization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this, this)

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

    // Text-to-Speech initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language for TTS
            val langResult = textToSpeech.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported or missing data", Toast.LENGTH_SHORT).show()
            }
            // TTS is initialized successfully
            isTtsInitialized = true
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Speak a given text
    fun speak(text: String) {
        if (isTtsInitialized) {
            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            // If TTS is not initialized, wait or handle error
            Toast.makeText(this, "Text-to-Speech not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    // Don't forget to shut down TTS when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
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
    val context = LocalContext.current
    val activity = context as MainActivity

    // Use TTS to speak the text when the screen loads
    LaunchedEffect(Unit) {
        if (activity.isTtsInitialized) {
            activity.speak("Press anywhere to proceed")
        } else {
            // Handle fallback if TTS is not initialized yet
            Toast.makeText(context, "TTS is initializing", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    navController.navigate("next")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Press anywhere to proceed")
        }
    }
}

@Composable
fun NextScreen() {
    val context = LocalContext.current
    val activity = context as MainActivity

    val scanSuccess = remember { mutableStateOf(false) }
    val capturedImage = remember { mutableStateOf<Bitmap?>(null) }
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri.value?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                capturedImage.value = bitmap
            }
        }
    }

    // LaunchedEffect to start listening and trigger TTS message for "scan"
    LaunchedEffect(Unit) {
        if (activity.isTtsInitialized) {
            activity.speak("Say 'scan' to take a photo.")
        } else {
            Toast.makeText(context, "TTS is initializing", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {

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
                        val photoFile = createImageFile(context)
                        imageUri.value = FileProvider.getUriForFile(
                            context,
                            "com.example.randomizer.fileprovider",
                            photoFile
                        )
                        takePictureLauncher.launch(imageUri.value)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            activity.startListening()
        } else {
            activity.checkMicrophonePermission()
        }
    }

    if (scanSuccess.value) {
        capturedImage.value?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "Captured Image")
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Say 'scan' to take a photo.")
        }
    }
}
  

// Helper function to create a file for storing the captured image
fun createImageFile(context: Context): File {
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile("photo_", ".jpg", storageDir)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RandomizerTheme {
        AppNavGraph()
    }
}
