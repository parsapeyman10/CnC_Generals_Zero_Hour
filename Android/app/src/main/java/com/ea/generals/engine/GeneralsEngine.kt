package com.ea.generals.engine

import kotlin.math.sqrt

/**
 * GeneralsEngine - Preserves PC's GameLogic structure in Kotlin
 * This mirrors the PC's architecture:
 *  PC:  GameLogic -> Object -> AIUpdate -> Locomotor
 *  Android: GeneralsEngine -> GameObject -> update()
 *
 *  PC INI structure is preserved via data classes
 */
class GeneralsEngine(
    val faction: String,
    val mapName: String
) {
    enum class Mode { SELECT, ATTACK, BUILD }

    var isPaused = false
    private var mode = Mode.SELECT
    private var gameTicks = 0

    // Resources - like PC's Money.cpp
    private var supplyCollected = 0

    // Objects on battlefield - like PC's Object list
    val objects = mutableListOf<GameObject>()
    var selected: GameObject? = null

    // Faction bonuses like PC's PlayerTemplate.ini
    private val factionBonus: FactionBonus = when (faction) {
        "USA" -> FactionBonus(incomeBonus = 1.1f, buildSpeed = 1.2f, powerBonus = 20)
        "China" -> FactionBonus(incomeBonus = 1.0f, buildSpeed = 1.0f, tankHpBonus = 1.3f)
        else -> FactionBonus(incomeBonus = 1.3f, buildSpeed = 0.9f, stealth = true) // GLA
    }

    init {
        // Initial base like PC's Starting building
        objects.add(GameObject("CommandCenter", 500f, 500f, isBuilding = true, hp = 2000, owner = faction, type = "hq"))
        objects.add(GameObject("SupplyCenter", 700f, 500f, isBuilding = true, hp = 1200, owner = faction, type = "supply"))
        // Enemy
        objects.add(GameObject("Enemy HQ", 1500f, 1500f, isBuilding = true, hp = 2000, owner = "Enemy", type = "hq"))
        objects.add(GameObject("Enemy Tank", 1400f, 1400f, isBuilding = false, hp = 300, owner = "Enemy", type = "tank"))
    }

    fun setMode(m: Mode) { mode = m }

    fun spawnUnit(type: String) {
        val center = objects.firstOrNull { it.type == "hq" } ?: return
        val obj = GameObject(
            name = when (type) {
                "dozer" -> "Dozer"
                else -> "Ranger"
            },
            x = center.x + 80, y = center.y + 80,
            isBuilding = false, hp = 200, owner = faction, type = type
        )
        objects.add(obj)
        selected = obj
    }

    fun spawnBuilding(type: String) {
        val center = objects.firstOrNull { it.type == "hq" } ?: return
        val obj = GameObject(
            name = type.replaceFirstChar { it.uppercase() },
            x = center.x + (Math.random().toFloat() * 200 - 100),
            y = center.y + 200,
            isBuilding = true, hp = 800, owner = faction, type = type
        )
        objects.add(obj)
    }

    fun onWorldTapped(worldX: Float, worldY: Float, isLongPress: Boolean) {
        if (isLongPress) {
            // Long press = move/attack like PC right click
            selected?.let { s ->
                if (mode == Mode.ATTACK) {
                    // Find enemy at tap
                    val target = objects.firstOrNull { !it.isBuilding && it.owner != faction && distance(it, worldX, worldY) < 60 }
                    if (target != null) {
                        s.target = target
                        s.state = "attacking"
                    } else {
                        s.targetX = worldX
                        s.targetY = worldY
                        s.state = "moving"
                    }
                } else {
                    s.targetX = worldX
                    s.targetY = worldY
                    s.state = "moving"
                    s.target = null
                }
            }
        } else {
            // Tap = select like PC left click
            val tapped = objects.firstOrNull { distance(it, worldX, worldY) < 50 }
            selected = tapped
        }
    }

    fun stopSelected() {
        selected?.let {
            it.state = "idle"
            it.target = null
            it.targetX = it.x
            it.targetY = it.y
        }
    }

    fun tick() {
        if (isPaused) return
        gameTicks++
        // Simple AI like PC's AIUpdate
        for (obj in objects) {
            if (obj.isBuilding) continue
            when (obj.state) {
                "moving" -> {
                    val dx = obj.targetX - obj.x
                    val dy = obj.targetY - obj.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist > 5) {
                        obj.x += dx / dist * 3f
                        obj.y += dy / dist * 3f
                    } else {
                        obj.state = "idle"
                    }
                }
                "attacking" -> {
                    val t = obj.target
                    if (t == null || t.hp <= 0) {
                        obj.state = "idle"
                        obj.target = null
                    } else {
                        // Move towards target if far
                        val dist = distance(obj, t.x, t.y)
                        if (dist > 80) {
                            obj.targetX = t.x
                            obj.targetY = t.y
                            obj.state = "moving"
                            // will continue attacking next tick
                            obj.target = t
                            obj.state = "attacking"
                        } else {
                            // In range, deal damage
                            t.hp -= 5
                            if (t.hp <= 0) {
                                objects.remove(t)
                                if (selected == t) selected = null
                            }
                        }
                        // Move a bit closer while attacking
                        if (dist > 60) {
                            val dx = t.x - obj.x
                            val dy = t.y - obj.y
                            obj.x += dx / dist * 1.5f
                            obj.y += dy / dist * 1.5f
                        }
                    }
                }
            }
        }

        // Income from supply like PC's Supply Truck
        if (gameTicks % 60 == 0) {
            supplyCollected += (100 * factionBonus.incomeBonus).toInt()
        }

        // Enemy simple AI
        if (gameTicks % 120 == 0) {
            val enemyTanks = objects.filter { it.owner == "Enemy" && !it.isBuilding }
            if (enemyTanks.size < 3 && gameTicks % 360 == 0) {
                objects.add(GameObject("Enemy Tank", 1500f, 1500f, false, 300, "Enemy", "tank"))
            }
            // Enemy attacks nearest
            for (et in enemyTanks) {
                if (et.state == "idle") {
                    val nearest = objects.filter { it.owner == faction }.minByOrNull { distance(et, it.x, it.y) }
                    nearest?.let {
                        et.target = it
                        et.state = "attacking"
                    }
                }
            }
        }
    }

    fun incomePerTick(): Int {
        val supplyCenters = objects.count { it.type == "supply" && it.owner == faction && it.hp > 0 }
        return supplyCenters * 25
    }

    fun powerStatus(): Int {
        val buildings = objects.count { it.isBuilding && it.owner == faction }
        return 80 + buildings * 20 + factionBonus.powerBonus.toInt()
    }

    fun gameTimeFormatted(): String {
        val totalSec = gameTicks / 60
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun distance(obj: GameObject, x: Float, y: Float): Float {
        val dx = obj.x - x
        val dy = obj.y - y
        return sqrt(dx * dx + dy * dy)
    }

    data class FactionBonus(
        val incomeBonus: Float = 1f,
        val buildSpeed: Float = 1f,
        val powerBonus: Float = 0f,
        val tankHpBonus: Float = 1f,
        val stealth: Boolean = false
    )
}

data class GameObject(
    var name: String,
    var x: Float,
    var y: Float,
    var isBuilding: Boolean,
    var hp: Int,
    var owner: String,
    var type: String,
    var maxHp: Int = hp,
    var state: String = "idle",
    var targetX: Float = x,
    var targetY: Float = y,
    var target: GameObject? = null
)
