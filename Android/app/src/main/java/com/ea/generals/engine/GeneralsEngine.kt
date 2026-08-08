package com.ea.generals.engine

import kotlin.math.sqrt

/**
 * GeneralsEngine - 8 Players Network Mode on گرگ‌میش
 * Preserves PC's GameLogic + Network lobby structure
 * Map: 8 start positions in circle, center sheep/wolves neutral
 */
class GeneralsEngine(
    val faction: String,
    val mapName: String,
    val enemyCount: Int = 3,
    val difficulty: String = "Medium",
    val aiFactions: List<String> = listOf("China","GLA","USA")
) {
    enum class Mode { SELECT, ATTACK, BUILD }

    var isPaused = false
    private var mode = Mode.SELECT
    private var gameTicks = 0
    private var supplyCollected = 0

    val objects = mutableListOf<GameObject>()
    var selected: GameObject? = null

    val sheep = mutableListOf<GameObject>()
    val wolves = mutableListOf<GameObject>()
    val players = mutableListOf<Player>()

    data class Player(
        val id: Int,
        val name: String,
        val faction: String,
        val isHuman: Boolean,
        val isAI: Boolean,
        var difficulty: String,
        val color: Int,
        var alive: Boolean = true,
        var team: Int = id
    )

    private val factionBonus: FactionBonus = when (faction) {
        "USA" -> FactionBonus(incomeBonus = 1.1f, buildSpeed = 1.2f, powerBonus = 20)
        "China" -> FactionBonus(incomeBonus = 1.0f, buildSpeed = 1.0f, tankHpBonus = 1.3f)
        else -> FactionBonus(incomeBonus = 1.3f, buildSpeed = 0.9f, stealth = true)
    }

    // 8 start positions for گرگ‌میش - circle around center (1000,1000) radius 750
    private val startPositions = listOf(
        Pair(1000f, 300f),   // 1 South - Human
        Pair(1550f, 550f),   // 2 SE
        Pair(1750f, 1000f),  // 3 East
        Pair(1550f, 1450f),  // 4 NE
        Pair(1000f, 1700f),  // 5 North
        Pair(450f, 1450f),   // 6 NW
        Pair(250f, 1000f),   // 7 West
        Pair(450f, 550f)     // 8 SW
    )

    private val playerColors = listOf(
        0xFF1976D2.toInt(), // Human blue
        0xFFD32F2F.toInt(), // Red
        0xFF388E3C.toInt(), // Green
        0xFFF57C00.toInt(), // Orange
        0xFF7B1FA2.toInt(), // Purple
        0xFF0097A7.toInt(), // Cyan
        0xFFFFEB3B.toInt(), // Yellow
        0xFF5D4037.toInt()  // Brown
    )

    init {
        initPlayers()
        if (mapName.contains("گرگ")) {
            initGorgeMish8P()
        } else {
            initDefaultMap()
        }
    }

    private fun initPlayers() {
        // Player 1 human
        players.add(Player(1, "YOU", faction, isHuman = true, isAI = false, difficulty = "Human", color = playerColors[0]))
        // AI enemies 2.. enemyCount+1
        for (i in 0 until enemyCount) {
            val aiFaction = aiFactions.getOrElse(i) { listOf("USA","China","GLA").random() }
            val color = playerColors[(i+1) % playerColors.size]
            players.add(Player(i+2, "AI ${i+2}", aiFaction, isHuman = false, isAI = true, difficulty = difficulty, color = color))
        }
        // Closed slots  (no player object, just empty)
    }

    private fun initGorgeMish8P() {
        // Create bases for each active player
        for (p in players) {
            val pos = startPositions[p.id - 1]
            // HQ
            objects.add(GameObject("${p.name} HQ", pos.first, pos.second, true, 2000, p.name, "hq", maxHp = 2000, ownerFaction = p.faction, playerId = p.id))
            // Supply
            objects.add(GameObject("${p.name} Supply", pos.first+70, pos.second, true, 1200, p.name, "supply", maxHp=1200, ownerFaction=p.faction, playerId=p.id))
            // Barracks
            objects.add(GameObject("${p.name} Barracks", pos.first, pos.second+70, true, 1000, p.name, "barracks", maxHp=1000, ownerFaction=p.faction, playerId=p.id))
            // Initial dozer/tank
            val initialUnit = if (p.isHuman) "Dozer" else "Tank"
            objects.add(GameObject("${p.name} $initialUnit", pos.first+80, pos.second+80, false, if(p.isHuman)200 else 300, p.name, if(p.isHuman)"dozer" else "tank", maxHp=if(p.isHuman)200 else 300, ownerFaction=p.faction, playerId=p.id))
        }

        // Neutral sheep in center (common resource to fight over)
        for (i in 0 until 8) {
            val s = GameObject("Sheep ${i+1}", 950f + (Math.random().toFloat()*140-70), 950f + (Math.random().toFloat()*140-70), false, 80, "Sheep", "sheep", maxHp=80, ownerFaction="Neutral", playerId=0)
            sheep.add(s); objects.add(s)
        }
        objects.add(GameObject("Shepherd", 1000f, 880f, false, 100, "Sheep", "shepherd", maxHp=100, ownerFaction="Neutral", playerId=0))

        // Wolves neutral creeps from north forest + den
        for (i in 0 until 4) {
            val w = GameObject("Wolf ${i+1}", 1000f + (Math.random().toFloat()*300-150), 1850f + (Math.random().toFloat()*100), false, 150, "Wolves", "wolf", maxHp=150, ownerFaction="Wolves", playerId=99)
            w.state="attacking"; w.target=sheep.randomOrNull()
            wolves.add(w); objects.add(w)
        }
        val alpha = GameObject("Alpha Wolf", 1000f, 1920f, false, 350, "Wolves", "alpha_wolf", maxHp=350, ownerFaction="Wolves", playerId=99)
        alpha.state="attacking"; alpha.target=sheep.firstOrNull()
        wolves.add(alpha); objects.add(alpha)
        objects.add(GameObject("Wolf Den", 1000f, 2050f, true, 1800, "Wolves", "den", maxHp=1800, ownerFaction="Wolves", playerId=99))
    }

    private fun initDefaultMap() {
        // Fallback single enemy
        for (p in players) {
            val pos = startPositions[p.id - 1]
            objects.add(GameObject("${p.name} HQ", pos.first, pos.second, true, 2000, p.name, "hq", maxHp=2000, ownerFaction=p.faction, playerId=p.id))
        }
    }

    fun setMode(m: Mode) { mode = m }

    fun spawnUnit(type: String) {
        val hq = objects.firstOrNull { it.playerId==1 && it.type=="hq" } ?: return
        val owner = players.first { it.id==1 }
        val obj = GameObject(
            name = when(type){"dozer"->"Dozer" else->"Ranger"},
            x=hq.x+80, y=hq.y+80, isBuilding=false, hp=200, owner=owner.name, type=type, maxHp=200, ownerFaction=owner.faction, playerId=1
        )
        objects.add(obj); selected=obj
    }

    fun spawnBuilding(type: String) {
        val hq = objects.firstOrNull { it.playerId==1 && it.type=="hq" } ?: return
        val owner = players.first { it.id==1 }
        val obj = GameObject(
            name=type.replaceFirstChar{it.uppercase()}, x=hq.x+(Math.random().toFloat()*200-100), y=hq.y+200,
            isBuilding=true, hp=if(type=="fence")400 else 800, owner=owner.name, type=type, maxHp=if(type=="fence")400 else 800, ownerFaction=owner.faction, playerId=1
        )
        if(type=="fence") obj.name="Fence"
        objects.add(obj)
    }

    fun onWorldTapped(worldX: Float, worldY: Float, isLongPress: Boolean) {
        if (isLongPress) {
            selected?.let { s ->
                // Only human units can be controlled
                if (s.playerId != 1) return
                if (mode==Mode.ATTACK) {
                    val target = objects.firstOrNull { it.playerId!=1 && it.playerId!=0 && it.playerId!=99 && distance(it,worldX,worldY)<70 }
                        ?: objects.firstOrNull { it.owner=="Wolves" && distance(it,worldX,worldY)<70 }
                    if (target!=null) { s.target=target; s.state="attacking" }
                    else { s.targetX=worldX; s.targetY=worldY; s.state="moving"; s.target=null }
                } else {
                    s.targetX=worldX; s.targetY=worldY; s.state="moving"; s.target=null
                }
            }
        } else {
            val tapped = objects.filter { distance(it,worldX,worldY)<60 }.minByOrNull { distance(it,worldX,worldY) }
            // Allow selecting only human and neutral sheep (to see), but not enemy directly unless attack mode
            // Actually allow selecting any for view
            selected = tapped
        }
    }

    fun stopSelected() { selected?.let{ it.state="idle"; it.target=null; it.targetX=it.x; it.targetY=it.y } }

    fun tick() {
        if (isPaused) return
        gameTicks++

        // Update all non-building units
        for (obj in objects.toList()) {
            if (obj.hp<=0) continue
            if (obj.isBuilding) continue
            if (obj.type=="sheep") {
                if (gameTicks%90==0 && obj.state=="idle" && Math.random()<0.3){
                    obj.targetX=950f+(Math.random().toFloat()*200-100); obj.targetY=950f+(Math.random().toFloat()*200-100); obj.state="moving"
                }
                val nearestWolf = wolves.filter{it.hp>0}.minByOrNull{ distance(it,obj.x,obj.y)}
                if (nearestWolf!=null && distance(nearestWolf,obj.x,obj.y)<130){
                    val dx=obj.x-nearestWolf.x; val dy=obj.y-nearestWolf.y; val d=sqrt(dx*dx+dy*dy)
                    if(d>1){ obj.targetX=obj.x+dx/d*90; obj.targetY=obj.y+dy/d*90; obj.state="moving" }
                }
            }
            when(obj.state){
                "moving" -> {
                    val dx=obj.targetX-obj.x; val dy=obj.targetY-obj.y; val d=sqrt(dx*dx+dy*dy)
                    val speed = when(obj.type){"wolf","alpha_wolf"->2.9f; "sheep"->1.9f; else-> if(obj.playerId==1)3f else difficultySpeed(obj) }
                    if(d>5){ obj.x+=dx/d*speed; obj.y+=dy/d*speed } else obj.state="idle"
                }
                "attacking" -> {
                    val t=obj.target
                    if(t==null || t.hp<=0 || t !in objects){ obj.state="idle"; obj.target=null }
                    else {
                        val d=distance(obj,t.x,t.y); val range= if(obj.type.contains("wolf"))42f else 85f
                        if(d>range){
                            obj.targetX=t.x; obj.targetY=t.y
                            val dx=t.x-obj.x; val dy=t.y-obj.y; val dd=sqrt(dx*dx+dy*dy)
                            val speed= if(obj.type.contains("wolf"))2.9f else difficultySpeed(obj)
                            obj.x+=dx/dd*speed; obj.y+=dy/dd*speed
                        } else {
                            val dmg = when(obj.type){
                                "alpha_wolf"-> if(difficulty=="Brutal")10 else 8
                                "wolf"-> if(difficulty=="Brutal")7 else 5
                                "tank"-> if(obj.playerId!=1 && difficulty=="Brutal")9 else 7
                                else->6
                            }
                            t.hp-=dmg
                            if(t.hp<=0){
                                objects.remove(t); sheep.remove(t); wolves.remove(t)
                                if(selected==t) selected=null
                                // Check if HQ destroyed -> player eliminated
                                if(t.type=="hq"){
                                    players.find{it.name==t.owner}?.alive=false
                                }
                                if(obj.type.contains("wolf")){
                                    obj.target= sheep.filter{it.hp>0}.minByOrNull{ distance(obj,it.x,it.y)} ?: objects.filter{it.playerId==1 && it.hp>0}.minByOrNull{ distance(obj,it.x,it.y)}
                                    if(obj.target==null) obj.state="idle"
                                } else obj.state="idle"
                            }
                        }
                    }
                }
            }
        }

        // Wolves AI
        for(w in wolves.filter{it.hp>0}){
            if(w.state=="idle" || w.target==null || w.target?.hp?:0<=0){
                w.target= sheep.filter{it.hp>0}.minByOrNull{ distance(w,it.x,it.y)} ?: objects.filter{it.playerId==1 && it.hp>0}.minByOrNull{ distance(w,it.x,it.y)}
                if(w.target!=null) w.state="attacking"
            }
        }

        // AI players logic (network mode)
        for(p in players.filter{it.isAI && it.alive}){
            // Simple AI: build, spawn, attack
            val pObjects = objects.filter{it.playerId==p.id}
            val pUnits = pObjects.filter{!it.isBuilding}
            val pBuildings = pObjects.filter{it.isBuilding}
            // Income tick already via supply

            // AI builds supply if less than 2
            if(gameTicks % (if(difficulty=="Brutal") 600 else if(difficulty=="Hard") 800 else 1100)==0){
                if(pBuildings.count{it.type=="supply"}<2 && pBuildings.isNotEmpty()){
                    val hq=pObjects.find{it.type=="hq"}?:continue
                    val nb=GameObject("${p.name} Supply", hq.x+(Math.random().toFloat()*180-90), hq.y+90, true,1200,p.name,"supply",maxHp=1200, ownerFaction=p.faction, playerId=p.id)
                    objects.add(nb)
                }
            }
            // AI spawns tank
            if(gameTicks % (if(difficulty=="Brutal") 400 else if(difficulty=="Hard") 550 else 750)==0){
                if(pUnits.size<6){
                    val hq=pObjects.find{it.type=="hq"}?:continue
                    val nt=GameObject("${p.name} Tank", hq.x+70+Math.random().toFloat()*40, hq.y+70, false,300,p.name,"tank",maxHp=300, ownerFaction=p.faction, playerId=p.id)
                    // AI tanks hunt nearest enemy (human or other AI or sheep)
                    val target = findNearestEnemy(nt)
                    if(target!=null){ nt.target=target; nt.state="attacking" }
                    objects.add(nt)
                }
            }
            // AI units attack logic
            for(u in pUnits.filter{it.state=="idle"}){
                val target=findNearestEnemy(u)
                if(target!=null){ u.target=target; u.state="attacking" }
                else if(Math.random()<0.02){
                    // Patrol
                    u.targetX=1000f+(Math.random().toFloat()*800-400); u.targetY=1000f+(Math.random().toFloat()*800-400); u.state="moving"
                }
            }
        }

        // Wolves reinforce
        if(mapName.contains("گرگ") && gameTicks%1400==0 && wolves.size<10){
            val nw=GameObject("Wolf",1000f+(Math.random().toFloat()*340-170),2050f,false,150,"Wolves","wolf",maxHp=150, ownerFaction="Wolves", playerId=99)
            nw.target= sheep.filter{it.hp>0}.randomOrNull() ?: objects.filter{it.playerId==1}.randomOrNull()
            nw.state="attacking"; wolves.add(nw); objects.add(nw)
        }
        if(gameTicks%60==0) supplyCollected+=(100*factionBonus.incomeBonus).toInt()
    }

    private fun difficultySpeed(obj: GameObject): Float {
        return when(difficulty){
            "Easy"->2.2f; "Hard"->3.2f; "Brutal"->3.6f; else->2.8f
        }
    }

    private fun findNearestEnemy(unit: GameObject): GameObject? {
        // For AI, enemies are all other players + sheep/wolves if close
        // For now, prioritize human, then nearest AI
        val enemies = objects.filter{
            it.playerId!=unit.playerId && it.playerId!=0 && it.playerId!=99 && it.hp>0 && !it.type.contains("sheep")
            || (it.owner=="Wolves" && unit.type!="sheep")
        }
        // Prefer human if close
        val humanTargets = enemies.filter{it.playerId==1}
        if(humanTargets.isNotEmpty() && Math.random()<0.7) {
            return humanTargets.minByOrNull{ distance(unit,it.x,it.y)}
        }
        return enemies.filter{it.hp>0}.minByOrNull{ distance(unit,it.x,it.y)}
            ?: sheep.filter{it.hp>0}.minByOrNull{ distance(unit,it.x,it.y)}
    }

    fun incomePerTick(): Int {
        val supplyCenters = objects.count{it.type=="supply" && it.playerId==1 && it.hp>0}
        return supplyCenters * 25
    }
    fun powerStatus(): Int {
        val buildings = objects.count{it.isBuilding && it.playerId==1}
        return 80+buildings*20+factionBonus.powerBonus.toInt()
    }
    fun gameTimeFormatted(): String { val s=ticksToSec(); return String.format("%02d:%02d",s/60,s%60) }
    private fun ticksToSec()= gameTicks/60
    fun sheepAlive()= sheep.count{it.hp>0}
    fun sheepTotal()= sheep.size
    fun wolvesAlive()= wolves.count{it.hp>0}
    fun enemiesAlive()= players.count{it.isAI && it.alive}
    fun totalPlayers()= players.size

    private fun distance(a:GameObject,x:Float,y:Float)= sqrt((a.x-x)*(a.x-x)+(a.y-y)*(a.y-y))

    data class FactionBonus(val incomeBonus:Float=1f,val buildSpeed:Float=1f,val powerBonus:Float=0f,val tankHpBonus:Float=1f,val stealth:Boolean=false)
}

data class GameObject(
    var name:String,var x:Float,var y:Float,var isBuilding:Boolean,var hp:Int,var owner:String,var type:String,
    var maxHp:Int=hp,var state:String="idle",var targetX:Float=x,var targetY:Float=y,var target: GameObject?=null,
    var ownerFaction:String = owner, var playerId:Int = 0
)
