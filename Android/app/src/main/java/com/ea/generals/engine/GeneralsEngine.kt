package com.ea.generals.engine

import kotlin.math.sqrt

/**
 * GeneralsEngine - Preserves PC's GameLogic structure in Kotlin
 * Map: گرگ‌میش (Wolf & Sheep) - Village Defense
 * Story: دهکده‌ای با گله گوسفند که گرگ‌ها حمله می‌کنند، تو باید با فکشن انتخابی دفاع کنی
 */
class GeneralsEngine(
    val faction: String,
    val mapName: String
) {
    enum class Mode { SELECT, ATTACK, BUILD }

    var isPaused = false
    private var mode = Mode.SELECT
    private var gameTicks = 0
    private var supplyCollected = 0

    val objects = mutableListOf<GameObject>()
    var selected: GameObject? = null

    // Sheep for گرگ‌میش map - the core mechanic
    val sheep = mutableListOf<GameObject>()
    val wolves = mutableListOf<GameObject>()

    private val factionBonus: FactionBonus = when (faction) {
        "USA" -> FactionBonus(incomeBonus = 1.1f, buildSpeed = 1.2f, powerBonus = 20)
        "China" -> FactionBonus(incomeBonus = 1.0f, buildSpeed = 1.0f, tankHpBonus = 1.3f)
        else -> FactionBonus(incomeBonus = 1.3f, buildSpeed = 0.9f, stealth = true)
    }

    init {
        if (mapName.contains("گرگ")) {
            initGorgeMishMap()
        } else {
            initDefaultMap()
        }
    }

    private fun initGorgeMishMap() {
        // Player base - روستا
        objects.add(GameObject("Command Center", 500f, 500f, true, 2000, faction, "hq"))
        objects.add(GameObject("Supply Center", 700f, 500f, true, 1200, faction, "supply"))
        objects.add(GameObject("Barracks", 500f, 700f, true, 1000, faction, "barracks"))
        // Dozer
        objects.add(GameObject("Dozer", 600f, 600f, false, 200, faction, "dozer"))

        // Sheep flock - گله وسط مپ (باید محافظت شوند)
        // These are neutral, wolves attack them, player must defend
        for (i in 0 until 8) {
            val s = GameObject(
                "Sheep ${i+1}", 950f + (Math.random().toFloat()*120-60), 950f + (Math.random().toFloat()*120-60),
                false, 80, "Sheep", "sheep"
            )
            s.maxHp = 80
            sheep.add(s)
            objects.add(s)
        }
        // Shepherd
        objects.add(GameObject("Shepherd", 1000f, 880f, false, 100, "Sheep", "shepherd"))

        // Wolves - گرگ‌ها از شمال می‌آیند
        for (i in 0 until 3) {
            val w = GameObject(
                "Wolf ${i+1}", 1000f + (Math.random().toFloat()*200-100), 1800f + (Math.random().toFloat()*100),
                false, 150, "Wolves", "wolf"
            )
            w.state = "attacking"
            w.target = sheep.random()
            wolves.add(w)
            objects.add(w)
        }
        // Wolf Alpha
        val alpha = GameObject("Alpha Wolf", 1000f, 1900f, false, 300, "Wolves", "alpha_wolf")
        alpha.state = "attacking"
        alpha.target = sheep.firstOrNull()
        wolves.add(alpha)
        objects.add(alpha)

        // Enemy outpost (optional)
        objects.add(GameObject("Wolf Den", 1000f, 2000f, true, 1500, "Wolves", "den"))
    }

    private fun initDefaultMap() {
        objects.add(GameObject("CommandCenter", 500f, 500f, true, 2000, faction, "hq"))
        objects.add(GameObject("SupplyCenter", 700f, 500f, true, 1200, faction, "supply"))
        objects.add(GameObject("Enemy HQ", 1500f, 1500f, true, 2000, "Enemy", "hq"))
        objects.add(GameObject("Enemy Tank", 1400f, 1400f, false, 300, "Enemy", "tank"))
    }

    fun setMode(m: Mode) { mode = m }

    fun spawnUnit(type: String) {
        val center = objects.firstOrNull { it.type == "hq" && it.owner == faction } ?: return
        val obj = GameObject(
            name = when (type) {
                "dozer" -> "Dozer"
                "wolf_hunter" -> "Wolf Hunter"
                else -> "Ranger"
            },
            x = center.x + 80, y = center.y + 80,
            isBuilding = false, hp = 200, owner = faction, type = type
        )
        objects.add(obj)
        selected = obj
    }

    fun spawnBuilding(type: String) {
        val center = objects.firstOrNull { it.type == "hq" && it.owner == faction } ?: return
        val obj = GameObject(
            name = type.replaceFirstChar { it.uppercase() },
            x = center.x + (Math.random().toFloat() * 200 - 100),
            y = center.y + 200,
            isBuilding = true, hp = 800, owner = faction, type = type
        )
        objects.add(obj)
        if (type == "fence") {
            obj.hp = 400
            obj.name = "Fence"
        }
    }

    fun onWorldTapped(worldX: Float, worldY: Float, isLongPress: Boolean) {
        if (isLongPress) {
            selected?.let { s ->
                if (mode == Mode.ATTACK) {
                    val target = objects.firstOrNull { it.owner == "Wolves" && distance(it, worldX, worldY) < 70 }
                    if (target != null) {
                        s.target = target
                        s.state = "attacking"
                    } else {
                        s.targetX = worldX; s.targetY = worldY; s.state = "moving"; s.target = null
                    }
                } else {
                    s.targetX = worldX; s.targetY = worldY; s.state = "moving"; s.target = null
                }
            }
        } else {
            val tapped = objects.firstOrNull { distance(it, worldX, worldY) < 55 }
            selected = tapped
        }
    }

    fun stopSelected() {
        selected?.let { it.state = "idle"; it.target = null; it.targetX = it.x; it.targetY = it.y }
    }

    fun tick() {
        if (isPaused) return
        gameTicks++

        // Update units
        for (obj in objects.toList()) {
            if (obj.hp <= 0) continue
            if (obj.isBuilding) continue
            // Sheep wander
            if (obj.type == "sheep") {
                if (gameTicks % 90 == 0 && obj.state == "idle") {
                    // Random wander but stay near center
                    if (Math.random() < 0.3) {
                        obj.targetX = 950f + (Math.random().toFloat()*200-100)
                        obj.targetY = 950f + (Math.random().toFloat()*200-100)
                        obj.state = "moving"
                    }
                }
                // Flee from wolves
                val nearestWolf = wolves.filter { it.hp > 0 }.minByOrNull { distance(it, obj.x, obj.y) }
                if (nearestWolf != null && distance(nearestWolf, obj.x, obj.y) < 120) {
                    // Flee opposite
                    val dx = obj.x - nearestWolf.x
                    val dy = obj.y - nearestWolf.y
                    val d = sqrt(dx*dx+dy*dy)
                    if (d > 1) {
                        obj.targetX = obj.x + dx/d * 80
                        obj.targetY = obj.y + dy/d * 80
                        obj.state = "moving"
                    }
                }
            }
            when (obj.state) {
                "moving" -> {
                    val dx = obj.targetX - obj.x
                    val dy = obj.targetY - obj.y
                    val dist = sqrt(dx*dx+dy*dy)
                    val speed = when (obj.type) {
                        "wolf", "alpha_wolf" -> 2.8f
                        "sheep" -> 1.8f
                        else -> 3f
                    }
                    if (dist > 5) { obj.x += dx/dist*speed; obj.y += dy/dist*speed } else { obj.state = "idle" }
                }
                "attacking" -> {
                    val t = obj.target
                    if (t == null || t.hp <= 0 || t !in objects) { obj.state = "idle"; obj.target = null }
                    else {
                        val dist = distance(obj, t.x, t.y)
                        val range = if (obj.type.contains("wolf")) 40f else 80f
                        if (dist > range) {
                            obj.targetX = t.x; obj.targetY = t.y
                            val dx = t.x - obj.x; val dy = t.y - obj.y
                            val d = sqrt(dx*dx+dy*dy)
                            val speed = if (obj.type.contains("wolf")) 2.8f else 2.5f
                            obj.x += dx/d*speed; obj.y += dy/d*speed
                        } else {
                            // Attack
                            val dmg = when (obj.type) {
                                "alpha_wolf" -> 8
                                "wolf" -> 5
                                else -> 7
                            }
                            t.hp -= dmg
                            if (t.hp <= 0) {
                                // Sheep killed?
                                if (t.type == "sheep") {
                                    // Wolf eats
                                }
                                objects.remove(t)
                                sheep.remove(t)
                                wolves.remove(t)
                                if (selected == t) selected = null
                                // Wolves pick new sheep
                                if (obj.type.contains("wolf")) {
                                    obj.target = sheep.filter { it.hp>0 }.minByOrNull { distance(obj, it.x, it.y) }
                                    if (obj.target == null) {
                                        // Attack player
                                        obj.target = objects.filter { it.owner==faction }.minByOrNull { distance(obj, it.x, it.y) }
                                    }
                                } else {
                                    obj.state = "idle"
                                    obj.target = null
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wolf AI: always hunt sheep, then player
        for (w in wolves.filter { it.hp>0 }) {
            if (w.state == "idle" || w.target == null || w.target?.hp ?:0 <=0) {
                w.target = sheep.filter { it.hp>0 }.minByOrNull { distance(w, it.x, it.y) }
                    ?: objects.filter { it.owner==faction && it.hp>0 }.minByOrNull { distance(w, it.x, it.y) }
                if (w.target != null) w.state = "attacking"
            }
        }

        // Spawn new wolves every ~25 sec
        if (mapName.contains("گرگ") && gameTicks % 1500 == 0 && wolves.size < 8) {
            val nw = GameObject("Wolf", 1000f + (Math.random().toFloat()*300-150), 2050f, false, 150, "Wolves", "wolf")
            nw.target = sheep.filter { it.hp>0 }.randomOrNull() ?: objects.firstOrNull { it.owner==faction }
            nw.state = "attacking"
            wolves.add(nw); objects.add(nw)
        }

        if (gameTicks % 60 == 0) supplyCollected += (100 * factionBonus.incomeBonus).toInt()
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
        val m = totalSec / 60; val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }
    fun sheepAlive(): Int = sheep.count { it.hp > 0 }
    fun sheepTotal(): Int = sheep.size
    fun wolvesAlive(): Int = wolves.count { it.hp > 0 }

    private fun distance(obj: GameObject, x: Float, y: Float): Float {
        val dx = obj.x - x; val dy = obj.y - y; return sqrt(dx*dx+dy*dy)
    }

    data class FactionBonus(val incomeBonus: Float=1f, val buildSpeed: Float=1f, val powerBonus: Float=0f, val tankHpBonus: Float=1f, val stealth: Boolean=false)
}

data class GameObject(
    var name: String, var x: Float, var y: Float, var isBuilding: Boolean, var hp: Int, var owner: String, var type: String,
    var maxHp: Int = hp, var state: String = "idle", var targetX: Float = x, var targetY: Float = y, var target: GameObject? = null
)
