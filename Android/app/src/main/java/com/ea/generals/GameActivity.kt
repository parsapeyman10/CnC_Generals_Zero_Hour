package com.ea.generals

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ea.generals.databinding.ActivityGameBinding
import com.ea.generals.engine.GeneralsEngine

/**
 * GameActivity - The actual RTS battlefield
 * This is where PC's GameLogic + GameClient run.
 * For now we use a 2D Canvas engine that preserves PC structure (resource, build, attack)
 * Future: replace GameView with GLES rendering of W3D meshes
 */
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var engine: GeneralsEngine
    private val handler = Handler(Looper.getMainLooper())
    private var credits = 10000
    private var power = 120

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val faction = intent.getStringExtra("faction") ?: "USA"
        val map = intent.getStringExtra("map") ?: "Twilight Flame"

        // Initialize engine with PC structure preserved
        engine = GeneralsEngine(faction, map)
        binding.gameView.setEngine(engine)
        binding.gameView.setFaction(faction)

        updateHud()

        // Simulate PC's resource tick
        handler.postDelayed(resourceTick, 2000)

        // HUD interactions
        binding.btnMenu.setOnClickListener {
            // Pause like PC ESC menu
            engine.isPaused = !engine.isPaused
            Toast.makeText(this, if (engine.isPaused) "Game Paused (ESC)" else "Resumed", Toast.LENGTH_SHORT).show()
        }

        binding.btnBuildDozer.setOnClickListener {
            if (credits >= 500) {
                credits -= 500
                engine.spawnUnit("dozer")
                updateHud()
                Toast.makeText(this, "Construction Dozer ready - Tap to place", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBuildSupply.setOnClickListener {
            if (credits >= 800) {
                credits -= 800
                engine.spawnBuilding("supply")
                updateHud()
            }
        }

        binding.btnBuildBarracks.setOnClickListener {
            if (credits >= 1000) {
                credits -= 1000
                engine.spawnBuilding("barracks")
                updateHud()
            }
        }

        binding.btnBuildWarFactory.setOnClickListener {
            if (credits >= 2000) {
                credits -= 2000
                engine.spawnBuilding("factory")
                updateHud()
            }
        }

        binding.btnAttack.setOnClickListener {
            engine.setMode(GeneralsEngine.Mode.ATTACK)
            Toast.makeText(this, "Select target - Attack mode", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            engine.setMode(GeneralsEngine.Mode.SELECT)
            engine.stopSelected()
        }

        // GameView callbacks
        binding.gameView.onCreditsChanged = { delta ->
            credits += delta
            updateHud()
        }
        binding.gameView.onMessage = { msg ->
            runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }

        Toast.makeText(this, "Commander $faction - Map: $map\nControls: Tap=Select LongPress=Move Pinch=Zoom", Toast.LENGTH_LONG).show()
    }

    private fun updateHud() {
        binding.tvCredits.text = "Credits: $credits $"
        binding.tvPower.text = "Power: +$power"
        binding.tvTime.text = engine.gameTimeFormatted()
    }

    private val resourceTick = object : Runnable {
        override fun run() {
            if (!engine.isPaused) {
                credits += engine.incomePerTick()
                power = engine.powerStatus()
                updateHud()
                binding.gameView.tick()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onPause() {
        super.onPause()
        engine.isPaused = true
        binding.gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        engine.isPaused = false
        binding.gameView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(resourceTick)
    }
}
