package com.ea.generals

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ea.generals.databinding.ActivityMenuBinding

/**
 * MenuActivity - Main menu exactly like PC Generals
 * Skirmish / Campaign / Multiplayer / Options / Exit
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        binding.btnSkirmish.setOnClickListener {
            startActivity(Intent(this, SkirmishActivity::class.java))
        }

        binding.btnCampaign.setOnClickListener {
            // For now campaign goes to same skirmish with story flavor
            val intent = Intent(this, SkirmishActivity::class.java)
            intent.putExtra("mode", "campaign")
            startActivity(intent)
        }

        binding.btnMultiplayer.setOnClickListener {
            // Toast like PC: GameSpy is discontinued
            android.widget.Toast.makeText(
                this,
                "Multiplayer: LAN only on Android (GameSpy discontinued like PC patch 1.04)",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }

        binding.btnOptions.setOnClickListener {
            startActivity(Intent(this, OptionsActivity::class.java))
        }

        binding.btnExit.setOnClickListener {
            finishAffinity()
        }
    }
}
