package com.example.randomizer
import okhttp3.*

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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

import android.speech.tts.TextToSpeech
import java.util.Locale
import android.speech.tts.UtteranceProgressListener



class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    // SpeechRecognizer instance to handle speech input
    lateinit var speechRecognizer: SpeechRecognizer

    lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this,this)

        // Request necessary permissions
        checkMicrophonePermission()
        checkCameraPermission()

        setContent {
            // Set the theme and initialize the navigation graph
            RandomizerTheme {
                AppNavGraph(textToSpeech)
            }
        }

        // Initialize the speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for TTS
            val result = textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported for TTS", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Initialization failed for TTS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop and shutdown TextToSpeech engine
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        // Stop listening when the activity is destroyed
        speechRecognizer.stopListening()
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
fun AppNavGraph(textToSpeech: TextToSpeech) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        // Define the composable destinations
        composable("main") { MainScreen(navController, textToSpeech) }
        composable("next") { NextScreen(textToSpeech) }
    }
}

// Main screen of the app
@Composable
fun MainScreen(navController: NavController, textToSpeech: TextToSpeech? = null) {

    val displayText = "Tap anywhere to proceed"

    // Use LaunchedEffect to trigger TTS when the composable enters the composition
    LaunchedEffect(Unit) {
        textToSpeech?.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "MainScreenText")
    }

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
            Text(text = displayText)
        }
    }
}

@Composable
fun NextScreen(textToSpeech: TextToSpeech) {
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
                sendBitmapToServer(bitmap)
                println("Bullshit")
                //needs to have some way to make a TTS response using the response from sendbitmaptoserver
            }
        }
    }

    val displayText = "Say 'scan' to take a photo."

    // Set up the speech recognition listener
    LaunchedEffect(Unit) {
        // Initialize the RecognitionListener
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
                        "${context.packageName}.provider",
                        photoFile
                    )
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        takePictureLauncher.launch(imageUri.value)
                    } else {
                        activity.checkCameraPermission()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Use LaunchedEffect to trigger TTS and start speech recognition after TTS finishes
    LaunchedEffect(Unit) {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                // TTS has finished speaking
                activity.runOnUiThread {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        activity.startListening()
                    } else {
                        activity.checkMicrophonePermission()
                    }
                }
            }

            override fun onError(utteranceId: String?) {}
        })

        // Speak the display text
        textToSpeech.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "NextScreenText")
    }

    // UI for the NextScreen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (capturedImage.value != null) {
            Image(
                bitmap = capturedImage.value!!.asImageBitmap(),
                contentDescription = "Captured Image"
            )
        } else {
            Text(text = displayText)
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

fun sendBitmapToServer(bitmap: Bitmap): Boolean {
    val url = "https://your-server-url.com/process_image"

    // Convert Bitmap to ByteArray
    val byteArray = bitmapToByteArray(bitmap)

    // Create multipart body to send the image
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", "image.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray))
        .build()

    // Prepare the HTTP request
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    // Execute the HTTP request synchronously
    val client = OkHttpClient()

    // This blocks the UI thread until the response is received
    try {
        val response = client.newCall(request).execute() // Blocking call
        return response.isSuccessful
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

// Convert Bitmap to byte array
fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}