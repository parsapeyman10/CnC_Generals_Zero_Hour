package com.ea.generals

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.ea.generals.databinding.ActivityMainBinding

/**
 * MainActivity - Splash screen like Generals PC intro
 * Preserves PC structure: Shows EA logo -> Generals logo -> Tap to start
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var canProceed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system UI for immersive like PC fullscreen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        // Animate tap to start like PC pulsating
        val pulse = AlphaAnimation(1.0f, 0.3f).apply {
            duration = 800
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }
        binding.tapToStart.startAnimation(pulse)

        // Simulate loading of INI/BIG files like PC does
        binding.engineInfo.text = "Loading INI • Maps • Shaders •  Mali-G71 Ready"
        Handler(Looper.getMainLooper()).postDelayed({
            binding.engineInfo.text = "PC Structure Preserved • GameLogic • GameClient • INI • Ready"
            canProceed = true
            binding.tapToStart.text = "TAP TO ENTER"
        }, 1500)

        binding.root.setOnClickListener {
            if (canProceed) proceedToMenu()
        }
        binding.tapToStart.setOnClickListener {
            if (canProceed) proceedToMenu()
        }
    }

    private fun proceedToMenu() {
        binding.tapToStart.clearAnimation()
        startActivity(Intent(this, MenuActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
