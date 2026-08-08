package com.ea.generals

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ea.generals.databinding.ActivitySkirmishBinding

/**
 * Skirmish - Network Mode 8 players on گرگ‌میش
 * Like PC's Network Lobby: choose enemy count 1-7 and difficulty
 */
class SkirmishActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkirmishBinding
    private var selectedFaction: String = "USA"
    private var enemyCount: Int = 3 // 1..7
    private var difficulty: String = "Medium"
    private val selectedMap = "گرگ‌میش"
    private val aiFactions = listOf("USA", "China", "GLA", "USA", "China", "GLA", "USA")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkirmishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        selectFaction("USA")
        updateEnemyCountUI()
        updateDifficultyUI()

        binding.cardUSA.setOnClickListener { selectFaction("USA") }
        binding.cardChina.setOnClickListener { selectFaction("China") }
        binding.cardGLA.setOnClickListener { selectFaction("GLA") }

        binding.btnMinus.setOnClickListener {
            if (enemyCount > 1) { enemyCount--; updateEnemyCountUI() }
        }
        binding.btnPlus.setOnClickListener {
            if (enemyCount < 7) { enemyCount++; updateEnemyCountUI() }
        }

        // Dots clickable 1..7
        binding.dot1.setOnClickListener { enemyCount = 1; updateEnemyCountUI() }
        binding.dot2.setOnClickListener { enemyCount = 2; updateEnemyCountUI() }
        binding.dot3.setOnClickListener { enemyCount = 3; updateEnemyCountUI() }
        binding.dot4.setOnClickListener { enemyCount = 4; updateEnemyCountUI() }
        binding.dot5.setOnClickListener { enemyCount = 5; updateEnemyCountUI() }
        binding.dot6.setOnClickListener { enemyCount = 6; updateEnemyCountUI() }
        binding.dot7.setOnClickListener { enemyCount = 7; updateEnemyCountUI() }

        binding.btnEasy.setOnClickListener { difficulty = "Easy"; updateDifficultyUI() }
        binding.btnMedium.setOnClickListener { difficulty = "Medium"; updateDifficultyUI() }
        binding.btnHard.setOnClickListener { difficulty = "Hard"; updateDifficultyUI() }
        binding.btnBrutal.setOnClickListener { difficulty = "Brutal"; updateDifficultyUI() }

        binding.btnStartGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("faction", selectedFaction)
            intent.putExtra("map", selectedMap)
            intent.putExtra("enemyCount", enemyCount)
            intent.putExtra("difficulty", difficulty)
            // Pass AI factions for slots 2..8
            intent.putStringArrayListExtra("aiFactions", ArrayList(aiFactions.take(enemyCount)))
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
        updateSlots()
    }

    private fun updateEnemyCountUI() {
        binding.tvEnemyCount.text = "تعداد دشمنان: $enemyCount (از 7) - ${enemyCount + 1} بازیکن از 8"
        // Update dots
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6, binding.dot7)
        dots.forEachIndexed { idx, tv ->
            val num = idx + 1
            if (num <= enemyCount) {
                tv.background = ContextCompat.getDrawable(this, R.drawable.bg_button_gold)
                tv.setTextColor(ContextCompat.getColor(this, R.color.black))
            } else {
                tv.background = ContextCompat.getDrawable(this, R.drawable.bg_panel)
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
        updateSlots()
        binding.btnStartGame.text = "START $selectedMap ($enemyCount دشمن - ${difficulty})"
    }

    private fun updateDifficultyUI() {
        val buttons = mapOf(
            "Easy" to binding.btnEasy,
            "Medium" to binding.btnMedium,
            "Hard" to binding.btnHard,
            "Brutal" to binding.btnBrutal
        )
        buttons.forEach { (key, btn) ->
            if (key == difficulty) {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.generals_gold)
                btn.setTextColor(ContextCompat.getColor(this, R.color.black))
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.generals_panel)
                btn.setTextColor(ContextCompat.getColor(this, R.color.generals_gold_light))
            }
        }
        updateSlots()
    }

    private fun updateSlots() {
        // Slot 1 is always human
        binding.slot1Text.text = "YOU ($selectedFaction)"

        // Slots 2..8
        val slotTexts = listOf(binding.slot2Text, binding.slot3Text, binding.slot4Text, binding.slot5Text, binding.slot6Text, binding.slot7Text, binding.slot8Text)
        val slots = listOf(binding.slot2, binding.slot3, binding.slot4, binding.slot5, binding.slot6, binding.slot7, binding.slot8)

        for (i in 0 until 7) {
            val idx = i + 2
            val isActive = i < enemyCount
            val slotLayout = slots[i]
            val slotText = slotTexts[i]
            if (isActive) {
                val aiFaction = aiFactions[i % aiFactions.size]
                slotText.text = "AI $idx ($aiFaction)"
                slotText.setTextColor(ContextCompat.getColor(this, R.color.white))
                (slotLayout.getChildAt(2) as? android.widget.TextView)?.text = difficulty
                (slotLayout.getChildAt(2) as? android.widget.TextView)?.setTextColor(ContextCompat.getColor(this, R.color.generals_gold))
                slotLayout.background = ContextCompat.getDrawable(this, R.drawable.bg_panel)
                slotLayout.alpha = 1f
            } else {
                slotText.text = "Closed"
                slotText.setTextColor(ContextCompat.getColor(this, R.color.generals_grey))
                (slotLayout.getChildAt(2) as? android.widget.TextView)?.text = "—"
                slotLayout.background = ContextCompat.getDrawable(this, R.drawable.bg_panel)
                slotLayout.alpha = 0.35f
            }
        }
    }
}
