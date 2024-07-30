package com.example.theniemannator

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var inputEditText: EditText
    private lateinit var vibrateButton: Button

    private lateinit var morseEncoder: MorseEncoder
    private lateinit var morseVibrator: MorseVibrator

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startService(serviceIntent)

        inputEditText = findViewById(R.id.inputEditText)
        vibrateButton = findViewById(R.id.vibrateButton)
        morseEncoder = MorseEncoder()
        morseVibrator = MorseVibrator(this)
        val speedSlider = findViewById<Slider>(R.id.slider)
        var speedSliderValue = 0f
        speedSlider.addOnChangeListener { _, value, _ ->
            speedSliderValue = value
        }
        vibrateButton.setOnClickListener {
            Log.d("MainActivity", "Button clicked")
            val input = inputEditText.text.toString()
            if (input.isNotEmpty()) {
                val morseCode = morseEncoder.encode(input)
                Log.d("MainActivity", "Morse code: $morseCode")
                lifecycleScope.launch {
                    try {
                        morseVibrator.vibrateMorseCode(morseCode, speedSliderValue)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error during vibration", e)
                    }
                }
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class MyForegroundService : Service() {
    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("MorseVibrator", "Background Service")

        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE for API level 31 and above
        )

        val notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Niemannator")
            .setContentText("Running in background")
            .build()
        startForeground(1, notification) // Start the service in foreground
        // ... Perform background tasks
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null // Not a bound service
    }
}

class MorseEncoder {
    private val morseCodeMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..",
        '9' to "----.", ' ' to "/"
    )

    fun encode(input: String): String {
        return input.uppercase()
            .map { char -> morseCodeMap[char] ?: "" }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}

class MorseVibrator(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var spaceBetweenSymbols = 450L
    private var spaceBetweenLetters = spaceBetweenSymbols * 2
    private var spaceBetweenWords = spaceBetweenSymbols * 5
    private var intensity = 255

    private var dotLength = 50L
    private var dashSpacing = 100L

    private val dot = longArrayOf(0, dotLength, 0, 0) // Start delay, on, off, on...
    private val dash = longArrayOf(0, dotLength, dashSpacing, dotLength) // Start delay, on, off, on...
    private val amplitudes = intArrayOf(0, intensity, 0, intensity) // Corresponding amplitudes
    private var dotEffect = VibrationEffect.createWaveform(dot, amplitudes, -1)
    private var dashEffect = VibrationEffect.createWaveform(dash, amplitudes, -1)

    @OptIn(UnstableApi::class)
    suspend fun vibrateMorseCode(morseCode: String, speedSliderValue: Float) = withContext(Dispatchers.Default) {
        //if(speedSliderValue > 0) {
        //    dashEffect = VibrationEffect.createWaveform(longArrayOf(0, dotLength, dashSpacing, dotLength), amplitudes, -1)
        //}

        Log.d("MorseVibrator", "Starting vibration for: $morseCode")
        for (symbol in morseCode) {
            when (symbol) {
                '.' -> {
                    vibrator.vibrate(dotEffect)
                }
                '-' -> {
                    vibrator.vibrate(dashEffect)
                }
                ' ' -> {
                    Log.d("MorseVibrator", "Pausing between letters")
                    delay(spaceBetweenLetters)
                }
                '/' -> {
                    Log.d("MorseVibrator", "Pausing between words")
                    delay(spaceBetweenWords)
                }
            }
            if (symbol == '.') {
                delay(spaceBetweenSymbols)
            }
            if(symbol == '-')  {
                delay(spaceBetweenSymbols + dashSpacing)
            }
        }
        Log.d("MorseVibrator", "Vibration complete")
    }
}