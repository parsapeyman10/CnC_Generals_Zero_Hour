package com.ea.generals

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ea.generals.databinding.ActivitySkirmishBinding

/**
 * SkirmishActivity - Faction & Map select like PC
 * USA / China / GLA - preserves PC's Generals Challenge structure
 */
class SkirmishActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkirmishBinding
    private var selectedFaction: String = "USA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkirmishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        val mode = intent.getStringExtra("mode") ?: "skirmish"
        if (mode == "campaign") {
            Toast.makeText(this, "Campaign: USA Mission 1 - Preserving PC story", Toast.LENGTH_SHORT).show()
        }

        // Default selection USA (like PC)
        selectFaction("USA")

        binding.cardUSA.setOnClickListener { selectFaction("USA") }
        binding.cardChina.setOnClickListener { selectFaction("China") }
        binding.cardGLA.setOnClickListener { selectFaction("GLA") }

        binding.btnStartGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("faction", selectedFaction)
            intent.putExtra("map", "Twilight Flame")
            startActivity(intent)
        }
    }

    private fun selectFaction(faction: String) {
        selectedFaction = faction

        // Reset all
        listOf(binding.cardUSA, binding.cardChina, binding.cardGLA).forEach {
            it.background = ContextCompat.getDrawable(this, R.drawable.bg_panel)
        }

        // Highlight selected with gold border
        val selectedCard = when (faction) {
            "USA" -> binding.cardUSA
            "China" -> binding.cardChina
            else -> binding.cardGLA
        }
        selectedCard.background = ContextCompat.getDrawable(this, R.drawable.bg_button_gold)

        // Update start button
        binding.btnStartGame.text = "START AS $faction"
    }
}
