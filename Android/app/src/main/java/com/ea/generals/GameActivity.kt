package com.ea.generals

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ea.generals.databinding.ActivityGameBinding
import com.ea.generals.engine.GeneralsEngine

/**
 * GameActivity - گرگ‌میش 8 Players Network Mode
 * Like PC's Network Skirmish: you vs 1-7 AI
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
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val faction = intent.getStringExtra("faction") ?: "USA"
        val map = intent.getStringExtra("map") ?: "گرگ‌میش"
        val enemyCount = intent.getIntExtra("enemyCount", 3)
        val difficulty = intent.getStringExtra("difficulty") ?: "Medium"
        val aiFactions = intent.getStringArrayListExtra("aiFactions") ?: arrayListOf("China","GLA","USA")

        engine = GeneralsEngine(faction, map, enemyCount, difficulty, aiFactions)
        binding.gameView.setEngine(engine)
        binding.gameView.setFaction(faction)

        updateHud()
        handler.postDelayed(resourceTick, 1000)

        binding.btnMenu.setOnClickListener {
            engine.isPaused = !engine.isPaused
            Toast.makeText(this, if (engine.isPaused) "Paused - گرگ‌میش ${enemyCount+1}P" else "Resumed", Toast.LENGTH_SHORT).show()
        }

        binding.btnBuildDozer.setOnClickListener {
            if (credits >= 500) { credits -= 500; engine.spawnUnit("dozer"); updateHud()
                Toast.makeText(this, "Dozer ready", Toast.LENGTH_SHORT).show()
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
            if (credits >= 800) { credits -= 800; engine.spawnBuilding("fence"); updateHud()
                Toast.makeText(this, "حصار ساخته شد", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAttack.setOnClickListener {
            engine.setMode(GeneralsEngine.Mode.ATTACK)
            Toast.makeText(this, "حالت حمله - دشمن را انتخاب کن", Toast.LENGTH_SHORT).show()
        }
        binding.btnStop.setOnClickListener { engine.setMode(GeneralsEngine.Mode.SELECT); engine.stopSelected() }

        binding.gameView.onCreditsChanged = { delta -> credits += delta; updateHud() }
        binding.gameView.onMessage = { msg -> runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }

        Toast.makeText(this, "گرگ‌میش 8P - $faction vs $enemyCount AI ($difficulty)\n${enemyCount+1} بازیکن - FFA\nTap=انتخاب LongPress=حرکت/حمله", Toast.LENGTH_LONG).show()
    }

    private fun updateHud() {
        binding.tvCredits.text = "Credits: $credits $"
        binding.tvPower.text = "Power: +$power"
        val aliveEnemies = engine.enemiesAlive()
        binding.tvTime.text = "${engine.gameTimeFormatted()} 🐑${engine.sheepAlive()}/${engine.sheepTotal()} 🐺${engine.wolvesAlive()} 👥${aliveEnemies} AI"
    }

    private val resourceTick = object : Runnable {
        override fun run() {
            if (!engine.isPaused) {
                credits += engine.incomePerTick()
                power = engine.powerStatus()
                updateHud()
                binding.gameView.tick()
                // Win/lose for 8P
                if (engine.sheepAlive() == 0) {
                    Toast.makeText(this@GameActivity, "شکست! گوسفندها خورده شدند", Toast.LENGTH_LONG).show()
                    engine.isPaused = true
                } else if (aliveEnemiesKilled()) {
                    Toast.makeText(this@GameActivity, "پیروزی! همه دشمنان نابود شدند 🎉", Toast.LENGTH_LONG).show()
                    engine.isPaused = true
                } else if (isHumanEliminated()) {
                    Toast.makeText(this@GameActivity, "شکست! پایگاه شما نابود شد", Toast.LENGTH_LONG).show()
                    engine.isPaused = true
                }
            }
            handler.postDelayed(this, 1000)
        }
        private fun aliveEnemiesKilled(): Boolean {
            // All AI HQs destroyed?
            return engine.players.filter{it.isAI}.all{ !it.alive } && engine.enemiesAlive()==0
        }
        private fun isHumanEliminated(): Boolean {
            val human = engine.players.find{it.isHuman}
            return human?.alive == false
        }
    }

    override fun onPause() { super.onPause(); engine.isPaused = true; binding.gameView.pause() }
    override fun onResume() { super.onResume(); engine.isPaused = false; binding.gameView.resume() }
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(resourceTick) }
}
