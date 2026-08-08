package com.ea.generals.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.ea.generals.engine.GameObject
import com.ea.generals.engine.GeneralsEngine

/**
 * GameView - 8 Players گرگ‌میش Network Mode
 * 8 bases around circle, center sheep/wolves neutral
 * Tap=Select, LongPress=Move/Attack, Drag=Scroll, Pinch=Zoom
 */
class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var engine: GeneralsEngine? = null
    private var faction: String = "USA"

    var onCreditsChanged: ((Int) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    private var camX = 1000f
    private var camY = 1050f
    private var zoom = 0.85f // Start more zoomed out for 8P

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 22f; typeface = Typeface.MONOSPACE
    }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 14f; typeface = Typeface.MONOSPACE
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(30, 0, 0, 0); strokeWidth = 1f; style = Paint.Style.STROKE
    }

    private var gestureDetector: GestureDetector
    private var scaleDetector: ScaleGestureDetector
    private var isLongPress = false

    // Colors for 8 players like PC lobby
    private val playerColors = mapOf(
        1 to Color.rgb(25,118,210), 2 to Color.rgb(211,47,47), 3 to Color.rgb(56,142,60),
        4 to Color.rgb(245,124,0), 5 to Color.rgb(123,31,162), 6 to Color.rgb(0,151,167),
        7 to Color.rgb(255,235,59), 8 to Color.rgb(93,64,55)
    )

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean { handleTap(e.x, e.y, false); return true }
            override fun onLongPress(e: MotionEvent) {
                isLongPress = true; handleTap(e.x, e.y, true)
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                camX += dx / zoom; camY += dy / zoom; clampCamera(); invalidate(); return true
            }
        })
        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoom = (zoom * d.scaleFactor).coerceIn(0.5f, 2.5f); invalidate(); return true
            }
        })
        setOnTouchListener { _, e ->
            scaleDetector.onTouchEvent(e); gestureDetector.onTouchEvent(e)
            if (e.action == MotionEvent.ACTION_DOWN) isLongPress = false
            true
        }
    }

    fun setEngine(e: GeneralsEngine) { engine = e }
    fun setFaction(f: String) { faction = f }
    fun tick() { invalidate() }
    fun pause() {}
    fun resume() {}

    private fun handleTap(sx: Float, sy: Float, longPress: Boolean) {
        val wx = (sx / zoom) + camX - (width / 2f / zoom)
        val wy = (sy / zoom) + camY - (height / 2f / zoom)
        engine?.onWorldTapped(wx, wy, longPress); invalidate()
    }
    private fun clampCamera() { camX = camX.coerceIn(200f, 1800f); camY = camY.coerceIn(200f, 1850f) }
    private fun worldToScreen(wx: Float, wy: Float): Pair<Float, Float> {
        return (wx - camX)*zoom + width/2f to (wy - camY)*zoom + height/2f
    }
    private fun colorFor(obj: GameObject): Int {
        if (obj.type=="sheep"||obj.type=="shepherd") return Color.WHITE
        if (obj.type=="wolf"||obj.type=="alpha_wolf"||obj.type=="den") return Color.rgb(90,90,90)
        if (obj.playerId==0) return Color.GRAY
        return playerColors[obj.playerId] ?: Color.GRAY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        // Terrain - bigger for 8P
        canvas.drawColor(Color.rgb(85, 107, 47))
        // Village area south
        val (villageSx, villageSy) = worldToScreen(650f, 500f)
        paint.color = Color.rgb(194, 178, 128); paint.style = Paint.Style.FILL
        canvas.drawCircle(villageSx, villageSy, 300f*zoom, paint)
        // Pasture center
        val (pastureSx, pastureSy) = worldToScreen(1000f, 1000f)
        paint.color = Color.rgb(120, 170, 80)
        canvas.drawCircle(pastureSx, pastureSy, 200f*zoom, paint)
        // Wolf forest north
        paint.color = Color.rgb(34, 60, 30)
        canvas.drawRect(0f, worldToScreen(1000f, 1750f).second - 350f*zoom, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(20, 40, 20)
        for (i in 0..9) {
            val tx = worldToScreen(300f + i*180f, 1900f).first
            canvas.drawCircle(tx, worldToScreen(0f,1900f).second, 20f*zoom, paint)
        }
        // Start position markers
        for (p in eng.players) {
            val pos = when(p.id){
                1->Pair(1000f,300f); 2->Pair(1550f,550f); 3->Pair(1750f,1000f); 4->Pair(1550f,1450f)
                5->Pair(1000f,1700f); 6->Pair(450f,1450f); 7->Pair(250f,1000f); 8->Pair(450f,550f); else->Pair(1000f,1000f)
            }
            val (sx,sy)=worldToScreen(pos.first,pos.second)
            paint.color = playerColors[p.id] ?: Color.GRAY
            paint.alpha = 30
            canvas.drawCircle(sx,sy,90f*zoom, paint)
            paint.alpha = 255
            // Player number
            smallText.color = playerColors[p.id] ?: Color.WHITE
            smallText.textSize = 10f*zoom
            smallText.textAlign = Paint.Align.CENTER
            canvas.drawText("${p.id}", sx, sy-60*zoom, smallText)
        }

        // Grid
        val gs = 100f*zoom
        val startX = (-camX*zoom + width/2f) % gs
        val startY = (-camY*zoom + height/2f) % gs
        var x = startX; while (x < width) { canvas.drawLine(x,0f,x,height.toFloat(), gridPaint); x+=gs }
        var y = startY; while (y < height) { canvas.drawLine(0f,y,width.toFloat(),y, gridPaint); y+=gs }

        // Draw objects
        for (obj in eng.objects) {
            val (sx, sy) = worldToScreen(obj.x, obj.y)
            val isSelected = eng.selected == obj

            // Shadow
            paint.color = Color.argb(60,0,0,0)
            canvas.drawOval(sx-20*zoom, sy+10*zoom, sx+20*zoom, sy+16*zoom, paint)
            paint.style = Paint.Style.FILL

            when (obj.type) {
                "sheep" -> {
                    paint.color = Color.WHITE; canvas.drawCircle(sx, sy, 13f*zoom, paint)
                    paint.color = Color.rgb(255,228,196); canvas.drawCircle(sx, sy+3*zoom, 6.5f*zoom, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(sx-3*zoom, sy+1*zoom, 1.8f*zoom, paint)
                    canvas.drawCircle(sx+3*zoom, sy+1*zoom, 1.8f*zoom, paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(sx-7*zoom, sy-6*zoom, 4.5f*zoom, paint)
                    canvas.drawCircle(sx+7*zoom, sy-6*zoom, 4.5f*zoom, paint)
                    canvas.drawCircle(sx, sy-9*zoom, 5.5f*zoom, paint)
                }
                "shepherd" -> {
                    paint.color = Color.rgb(139,69,19); canvas.drawCircle(sx, sy, 11f*zoom, paint)
                    paint.color = Color.RED; canvas.drawRect(sx-7*zoom, sy-17*zoom, sx+7*zoom, sy-9*zoom, paint)
                }
                "wolf" -> {
                    paint.color = Color.rgb(90,90,90)
                    val path = Path().apply {
                        moveTo(sx, sy-13*zoom); lineTo(sx+12*zoom, sy+6*zoom)
                        lineTo(sx+5*zoom, sy+13*zoom); lineTo(sx-5*zoom, sy+13*zoom)
                        lineTo(sx-12*zoom, sy+6*zoom); close()
                    }
                    canvas.drawPath(path, paint)
                    paint.color = Color.RED
                    canvas.drawCircle(sx-4*zoom, sy-2*zoom, 2.2f*zoom, paint)
                    canvas.drawCircle(sx+4*zoom, sy-2*zoom, 2.2f*zoom, paint)
                }
                "alpha_wolf" -> {
                    paint.color = Color.rgb(40,40,40)
                    val path = Path().apply {
                        moveTo(sx, sy-17*zoom); lineTo(sx+15*zoom, sy+7*zoom)
                        lineTo(sx+7*zoom, sy+17*zoom); lineTo(sx-7*zoom, sy+17*zoom)
                        lineTo(sx-15*zoom, sy+7*zoom); close()
                    }
                    canvas.drawPath(path, paint)
                    paint.color = Color.RED
                    canvas.drawCircle(sx-5*zoom, sy-2*zoom, 2.8f*zoom, paint)
                    canvas.drawCircle(sx+5*zoom, sy-2*zoom, 2.8f*zoom, paint)
                    paint.color = Color.YELLOW; canvas.drawCircle(sx, sy-19*zoom, 4.5f*zoom, paint)
                }
                "den" -> {
                    paint.color = Color.rgb(50,30,15)
                    canvas.drawOval(sx-48*zoom, sy-28*zoom, sx+48*zoom, sy+28*zoom, paint)
                    paint.color = Color.BLACK
                    canvas.drawOval(sx-18*zoom, sy-9*zoom, sx+18*zoom, sy+14*zoom, paint)
                }
                else -> {
                    val baseColor = colorFor(obj)
                    paint.color = baseColor
                    if (obj.isBuilding) {
                        val w=58*zoom; val h=48*zoom
                        canvas.drawRect(sx-w/2, sy-h/2, sx+w/2, sy+h/2, paint)
                        // Player color stripe on top
                        paint.color = baseColor
                        canvas.drawRect(sx-w/2, sy-h/2, sx+w/2, sy-h/2+7*zoom, paint)
                        paint.color = Color.argb(80,255,255,255)
                        canvas.drawRect(sx-w/2, sy-h/2, sx+w/2, sy-h/2+7*zoom, paint)
                        // ID
                        smallText.color = Color.WHITE; smallText.textSize=8f*zoom; smallText.textAlign=Paint.Align.CENTER
                        canvas.drawText("${obj.playerId}", sx, sy+4*zoom, smallText)
                    } else {
                        val path = Path().apply {
                            moveTo(sx, sy-15*zoom); lineTo(sx+13*zoom, sy); lineTo(sx, sy+15*zoom); lineTo(sx-13*zoom, sy); close()
                        }
                        canvas.drawPath(path, paint)
                        paint.color = Color.rgb(30,30,30)
                        canvas.drawCircle(sx,sy,5*zoom, paint)
                        // Small player dot
                        paint.color = baseColor
                        canvas.drawCircle(sx+8*zoom, sy-8*zoom, 4*zoom, paint)
                    }
                }
            }

            // Selection
            if (isSelected) {
                val isOwn = obj.playerId==1
                paint.color = if(isOwn) Color.GREEN else Color.RED
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f*zoom
                if (obj.isBuilding||obj.type=="den") canvas.drawRect(sx-32*zoom, sy-28*zoom, sx+32*zoom, sy+28*zoom, paint)
                else canvas.drawCircle(sx,sy,21*zoom, paint)
                paint.style = Paint.Style.FILL
            }

            // HP bar
            if (obj.type!="sheep" || obj.hp < obj.maxHp) {
                val pct = obj.hp.toFloat()/obj.maxHp.toFloat().coerceAtLeast(1f)
                val bw=38*zoom; val bh=3.5f*zoom; val bx=sx-bw/2; val by=sy-(if(obj.isBuilding||obj.type=="den")30 else 24)*zoom
                paint.color = Color.BLACK; canvas.drawRect(bx,by,bx+bw,by+bh, paint)
                paint.color = when { pct>0.6->Color.GREEN; pct>0.3->Color.YELLOW; else->Color.RED }
                canvas.drawRect(bx,by,bx+bw*pct.coerceIn(0f,1f),by+bh, paint)
            }

            // Label
            smallText.textAlign = Paint.Align.CENTER
            if (obj.type=="sheep") {
                smallText.textSize = 7f*zoom; smallText.color=Color.WHITE
                smallText.setShadowLayer(2f,1f,1f, Color.BLACK)
                canvas.drawText("🐑", sx, sy+25*zoom, smallText)
                smallText.clearShadowLayer()
            } else if (obj.type=="wolf"||obj.type=="alpha_wolf") {
                smallText.textSize = 8f*zoom; smallText.color=Color.WHITE
                smallText.setShadowLayer(2f,1f,1f, Color.BLACK)
                canvas.drawText(if(obj.type=="alpha_wolf")"گرگ آلفا" else "گرگ", sx, sy+28*zoom, smallText)
                smallText.clearShadowLayer()
            } else {
                // Show owner for buildings
                smallText.textSize = 8f*zoom; smallText.color=Color.WHITE
                smallText.setShadowLayer(2f,1f,1f, Color.BLACK)
                val label = if(obj.isBuilding) "${obj.owner} ${obj.type}" else obj.name
                // Truncate
                canvas.drawText(label, sx, sy+28*zoom, smallText)
                smallText.clearShadowLayer()
            }

            if (obj.state=="attacking" && obj.target!=null) {
                paint.color = Color.RED; paint.strokeWidth=1.8f; paint.style= Paint.Style.STROKE
                val (tx,ty)=worldToScreen(obj.target!!.x, obj.target!!.y)
                canvas.drawLine(sx,sy,tx,ty, paint)
                paint.style= Paint.Style.FILL; canvas.drawCircle(tx,ty,3.5f*zoom, paint)
            }
        }

        // HUD overlays
        textPaint.textSize = 10f; textPaint.color = Color.YELLOW; textPaint.textAlign=Paint.Align.LEFT
        canvas.drawText("گرگ‌میش 8P • 🐑${eng.sheepAlive()}/${eng.sheepTotal()} 🐺${eng.wolvesAlive()} 👥${eng.enemiesAlive()} AI", 12f, 20f, textPaint)

        textPaint.textSize = 9f; textPaint.color = Color.argb(170,255,255,255)
        canvas.drawText(if(eng.isPaused)"PAUSED" else "Tap=انتخاب LongPress=حمله Drag=جابجایی Pinch=زوم", 12f, height-90f, textPaint)

        // Player status top right
        var yOff=38f
        for(p in eng.players){
            val col = playerColors[p.id] ?: Color.GRAY
            paint.color = col; canvas.drawCircle(width-14f, yOff, 6f, paint)
            smallText.textAlign = Paint.Align.RIGHT; smallText.textSize=8f; smallText.color= if(p.isHuman) Color.GREEN else Color.WHITE
            val status = if(!p.alive) "💀" else if(p.isHuman) "YOU" else p.difficulty.take(1)
            canvas.drawText("${p.name} ${p.faction} $status", width-26f, yOff+3f, smallText)
            yOff+=14f
        }

        if (eng.selected==null) {
            textPaint.color = Color.YELLOW; textPaint.textSize=12f; textPaint.textAlign=Paint.Align.CENTER
            canvas.drawText("یک واحد را لمس کن - 8 نفره FFA!", width/2f, 56f, textPaint)
            textPaint.textAlign=Paint.Align.LEFT
        }

        // Map labels
        smallText.textAlign=Paint.Align.CENTER
        smallText.color=Color.rgb(60,40,10); smallText.textSize=10f*zoom
        var (vx,vy)=worldToScreen(650f,500f); canvas.drawText("روستا", vx, vy-60*zoom, smallText)
        var (px,py)=worldToScreen(1000f,1000f); smallText.color=Color.rgb(34,100,34); canvas.drawText("مرتع", px, py-110*zoom, smallText)
        var (wx,wy)=worldToScreen(1000f,2050f); smallText.color=Color.RED; canvas.drawText("لانه", wx, wy-40*zoom, smallText)
    }
}
