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
 * GameActivity - گرگ‌میش battlefield
 * PC structure preserved, map is گرگ‌میش
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
        val map = intent.getStringExtra("map") ?: "گرگ‌میش"

        engine = GeneralsEngine(faction, map)
        binding.gameView.setEngine(engine)
        binding.gameView.setFaction(faction)

        updateHud()
        handler.postDelayed(resourceTick, 2000)

        binding.btnMenu.setOnClickListener {
            engine.isPaused = !engine.isPaused
            Toast.makeText(this, if (engine.isPaused) "Paused - گرگ‌میش" else "Resumed", Toast.LENGTH_SHORT).show()
        }

        binding.btnBuildDozer.setOnClickListener {
            if (credits >= 500) { credits -= 500; engine.spawnUnit("dozer"); updateHud()
                Toast.makeText(this, "Dozer ready - گرگ‌ها نزدیکند!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "بودجه کم است", Toast.LENGTH_SHORT).show()
        }
        binding.btnBuildSupply.setOnClickListener {
            if (credits >= 800) { credits -= 800; engine.spawnBuilding("supply"); updateHud() }
            else Toast.makeText(this, "بودجه کم است", Toast.LENGTH_SHORT).show()
        }
        binding.btnBuildBarracks.setOnClickListener {
            if (credits >= 1000) { credits -= 1000; engine.spawnBuilding("barracks"); updateHud() }
            else Toast.makeText(this, "بودجه کم است", Toast.LENGTH_SHORT).show()
        }
        binding.btnBuildWarFactory.setOnClickListener {
            // In گرگ‌میش this is Fence
            if (credits >= 800) { credits -= 800; engine.spawnBuilding("fence"); updateHud()
                Toast.makeText(this, "حصار ساخته شد - از گله محافظت می‌کند", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAttack.setOnClickListener {
            engine.setMode(GeneralsEngine.Mode.ATTACK)
            Toast.makeText(this, "حالت حمله - گرگ را انتخاب کن", Toast.LENGTH_SHORT).show()
        }
        binding.btnStop.setOnClickListener { engine.setMode(GeneralsEngine.Mode.SELECT); engine.stopSelected() }

        binding.gameView.onCreditsChanged = { delta -> credits += delta; updateHud() }
        binding.gameView.onMessage = { msg -> runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }

        Toast.makeText(this, "گرگ‌میش - فرمانده $faction\nاز 8 گوسفند محافظت کن! گرگ‌ها از شمال می‌آیند\nTap=انتخاب LongPress=حرکت/حمله", Toast.LENGTH_LONG).show()
    }

    private fun updateHud() {
        binding.tvCredits.text = "Credits: $credits $"
        binding.tvPower.text = "Power: +$power"
        binding.tvTime.text = "${engine.gameTimeFormatted()}  🐑${engine.sheepAlive()}/${engine.sheepTotal()} 🐺${engine.wolvesAlive()}"
    }

    private val resourceTick = object : Runnable {
        override fun run() {
            if (!engine.isPaused) {
                credits += engine.incomePerTick()
                power = engine.powerStatus()
                updateHud()
                binding.gameView.tick()
                // Check win/lose for گرگ‌میش
                if (engine.sheepAlive() == 0) {
                    Toast.makeText(this@GameActivity, "شکست! همه گوسفندها خورده شدند - گرگ‌ها پیروز شدند", Toast.LENGTH_LONG).show()
                    engine.isPaused = true
                } else if (engine.wolvesAlive() == 0 && engine.gameTimeFormatted() > "03:00" && engine.sheepAlive() >= 5) {
                    // After 3 min if wolves cleared and sheep saved
                    // not auto win, just hint
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onPause() { super.onPause(); engine.isPaused = true; binding.gameView.pause() }
    override fun onResume() { super.onResume(); engine.isPaused = false; binding.gameView.resume() }
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(resourceTick) }
}
