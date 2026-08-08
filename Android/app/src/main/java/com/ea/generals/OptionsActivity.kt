package com.ea.generals

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ea.generals.databinding.ActivityOptionsBinding

class OptionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        binding.sliderGraphics.addOnChangeListener { _, value, _ ->
            val label = when {
                value < 0.3 -> "Graphics: Low (A30 Battery Saver)"
                value < 0.7 -> "Graphics: Medium (A30 Optimized)"
                else -> "Graphics: High (PC Quality - May lag on A30)"
            }
            binding.tvGraphics.text = label
        }

        binding.btnBack.setOnClickListener { finish() }
    }
}
