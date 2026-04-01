package com.example.agriverse.MainPage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.agriverse.Profile.ProfileActivity
import com.example.agriverse.R
import com.example.agriverse.ai.StudentLearningActivity

import com.example.agriverse.plantdig.PlantDiagnoseActivity
import com.example.agriverse.cropframlearning.WeatherCropRec
import com.example.agriverse.cropframlearning.WeatherRetrofit
import com.example.agriverse.login.LoginActivity
import com.example.agriverse.mainspeechai.Speechlanch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.collections.get

const val WEATHER_API_KEY = "Put Api"

class MainActivityWeather : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvWeatherResult: TextView
    private lateinit var profileImage: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        
        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Make StatusBar Transparent and match Header
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#2E7D32")
        }

        setContentView(R.layout.activity_main_weather)

        // 🔹 UI references
        val cardDiagnose = findViewById<CardView>(R.id.cardDiagnose)
        val weatherCard = findViewById<CardView>(R.id.weatherCard)
        val cardFarmer = findViewById<CardView>(R.id.cardFarmer)
        val cardStudent = findViewById<CardView>(R.id.cardStudent)
        val cardMarket = findViewById<CardView>(R.id.cardMarket)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val cardAiAssistant = findViewById<CardView>(R.id.cardAiAssistant)
        profileImage = findViewById<ImageView>(R.id.profileImage)
        val tvAppSlogan = findViewById<TextView>(R.id.tvAppSlogan)

        // Personalize slogan if user name is available
        val user = auth.currentUser
        if (user != null) {
            val name = user.displayName ?: user.email?.split("@")?.get(0) ?: "Farmer"
            tvAppSlogan.text = "Welcome back, ${name.toUpperCase(Locale.ROOT)}!"
        }

        // Force original colors for bottom nav icons
        bottomNav.itemIconTintList = null

        tvWeatherResult = findViewById<TextView>(R.id.tvWeatherResult)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        profileImage.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Plant Diagnose (Camera)
        cardDiagnose.setOnClickListener {
            startActivity(Intent(this, PlantDiagnoseActivity::class.java))
        }

        // 2️⃣ Weather Card
        weatherCard.setOnClickListener {
            Toast.makeText(this, "🌦 Fetching Weather...", Toast.LENGTH_SHORT).show()
            fetchWeather()
        }

        // 3️⃣ Farmer Learning
        cardFarmer.setOnClickListener {
            startActivity(Intent(this, WeatherCropRec::class.java))
        }

        // 4️⃣ Student Learning
        cardStudent.setOnClickListener {

            startActivity(Intent(this, StudentLearningActivity::class.java))

        }

        // 5️⃣ Market & Govt Info
        cardMarket.setOnClickListener {
            val url = "https://agmarknet.gov.in/home"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }


        cardAiAssistant.setOnClickListener {
            startActivity(Intent(this, Speechlanch::class.java))
        }
        

        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {

                R.id.nav_diagnose -> {
                    startActivity(Intent(this, PlantDiagnoseActivity::class.java))
                    true
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, Speechlanch::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchWeather()
        } else {
            Toast.makeText(this, "Permission denied! Cannot fetch location.", Toast.LENGTH_SHORT).show()
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

    private fun fetchWeather() {
        if (!checkLocationPermission()) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                tvWeatherResult.text = "❌ Location not found. Please try again."
                return@addOnSuccessListener
            }

            lifecycleScope.launch {
                try {
                    val weather = WeatherRetrofit.api.getWeather(
                        location.latitude,
                        location.longitude,
                        WEATHER_API_KEY
                    )

                    val text = "<b>📍 Location:</b> ${weather.name}<br>" +
                            "<b>🌡 Temperature:</b> ${weather.main.temp}°C<br>" +
                            "<b>💧 Humidity:</b> ${weather.main.humidity}%<br>" +
                            "<b>🌤 Weather:</b> ${weather.weather[0].description}"

                    tvWeatherResult.text = fromHtmlCompat(text)

                } catch (e: Exception) {
                    e.printStackTrace()
                    tvWeatherResult.text = "❌ Error fetching weather: ${e.localizedMessage}"
                }
            }
        }
    }
}
