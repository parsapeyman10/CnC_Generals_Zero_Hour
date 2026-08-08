package com.ea.generals

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ea.generals.databinding.ActivitySkirmishBinding

/**
 * SkirmishActivity - Faction & Map select like PC
 * Map is now گرگ‌میش (Wolf & Sheep) as requested
 */
class SkirmishActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkirmishBinding
    private var selectedFaction: String = "USA"
    private val selectedMap = "گرگ‌میش" // Fixed as requested

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkirmishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        val mode = intent.getStringExtra("mode") ?: "skirmish"
        if (mode == "campaign") {
            Toast.makeText(this, "Campaign: گرگ‌میش - Village Defense Story", Toast.LENGTH_SHORT).show()
        }

        selectFaction("USA")

        binding.cardUSA.setOnClickListener { selectFaction("USA") }
        binding.cardChina.setOnClickListener { selectFaction("China") }
        binding.cardGLA.setOnClickListener { selectFaction("GLA") }

        binding.btnStartGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("faction", selectedFaction)
            intent.putExtra("map", selectedMap)
            startActivity(intent)
        }
    }

    private fun selectFaction(faction: String) {
        selectedFaction = faction

        listOf(binding.cardUSA, binding.cardChina, binding.cardGLA).forEach {
            it.background = ContextCompat.getDrawable(this, R.drawable.bg_panel)
        }

        val selectedCard = when (faction) {
            "USA" -> binding.cardUSA
            "China" -> binding.cardChina
            else -> binding.cardGLA
        }
        selectedCard.background = ContextCompat.getDrawable(this, R.drawable.bg_button_gold)
        binding.btnStartGame.text = "START $selectedMap AS $faction"
    }
}
