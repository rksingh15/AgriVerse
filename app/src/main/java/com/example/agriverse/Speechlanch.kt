package com.example.agriverse.mainspeechai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.agriverse.R
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.launch
import java.util.Locale

class Speechlanch : AppCompatActivity() {

    private lateinit var imgMic: FrameLayout
    private lateinit var txtAssistant: TextView

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    // Firebase Gemini Model - Optimized Identity & Model Name
    private val model = Firebase.ai(
        backend = GenerativeBackend.googleAI()
    ).generativeModel(
        modelName = "gemini-2.5-flash-lite",
        systemInstruction = content {
            text("Your name is Julia AI. You are a helpful assistant for Indian farmers. Only if someone asks 'who are you', 'your name', or 'who created you', say 'Mera naam Julia AI hai aur mujhe Ritik ne banaya hai'. For all other agricultural or general questions, provide direct, simple, and helpful answers. Do not mention Ritik unless specifically asked about yourself. Avoid using too many special characters in your response.")
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speechlanch)

        imgMic = findViewById(R.id.micContainer)
        txtAssistant = findViewById(R.id.tvTitle)

        requestMicPermission()
        setupSpeechRecognizer()
        setupTTS()

        imgMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                speechRecognizer.startListening(speechIntent)
            } else {
                requestMicPermission()
            }
        }
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                txtAssistant.text = "Listening 🎧..."
            }

            override fun onResults(results: Bundle?) {
                val userText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                if (!userText.isNullOrEmpty()) {
                    txtAssistant.text = "You: $userText"
                    askFirebaseAI(userText)
                } else {
                    txtAssistant.text = "Sorry, I didn't catch that."
                }
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                txtAssistant.text = "Error: $message"
                Log.e("Speechlanch", "Recognizer Error: $message")
                
                if (error == SpeechRecognizer.ERROR_CLIENT) {
                   setupSpeechRecognizer()
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun askFirebaseAI(question: String) {
        lifecycleScope.launch {
            txtAssistant.text = "Thinking 🤔..."
            try {
                val response = model.generateContent(content { text(question) })
                val answer = response.text ?: "No answer available"
                
                // Formatting for UI (Displaying Bolds and Brs)
                val answerHtml = answer
                    .replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")
                    .replace(Regex("\\* "), "<br>• ")
                    .replace("###", "<br><b>")

                val displayText = "<b>🤖 Julia:</b><br>${answerHtml}"
                txtAssistant.text = fromHtmlCompat(displayText)

                // CLEANING for Voice (Removing symbols so they aren't spoken)
                val cleanSpeechText = answer
                    .replace("*", "")
                    .replace("#", "")
                    .replace("_", "")
                    .replace(":", "")
                    .replace("|", "")
                    .replace(",", "")
                    .trim()

                speak(cleanSpeechText)
            } catch (e: Exception) {
                Log.e("Speechlanch", "AI Error", e)
                txtAssistant.text = "AI Error: ${e.localizedMessage}"
                speak("Error")
            }
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {

                tts.setPitch(1.1f)      // Clear female-like pitch
                tts.setSpeechRate(0.9f) // Better clarity speed
                isTtsReady = true
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SpeechOutput")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

    private fun fromHtmlCompat(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }
}
