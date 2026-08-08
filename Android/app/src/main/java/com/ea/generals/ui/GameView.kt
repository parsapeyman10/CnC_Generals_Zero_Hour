package com.ea.generals.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.ea.generals.R
import com.ea.generals.engine.GameObject
import com.ea.generals.engine.GeneralsEngine

/**
 * GameView - گرگ‌میش map specialized
 * روستا در مرکز، گله گوسفند، گرگ‌ها از شمال
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
    private var camY = 1150f
    private var zoom = 1f

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
                zoom = (zoom * d.scaleFactor).coerceIn(0.6f, 2.5f); invalidate(); return true
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
    private fun clampCamera() { camX = camX.coerceIn(400f, 1600f); camY = camY.coerceIn(500f, 1750f) }
    private fun worldToScreen(wx: Float, wy: Float): Pair<Float, Float> {
        return (wx - camX)*zoom + width/2f to (wy - camY)*zoom + height/2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        // Terrain - گرگ‌میش: چمن روستا + خاک اطراف + جنگل شمال (گرگ‌ها)
        // Base grass
        canvas.drawColor(Color.rgb(85, 107, 47))
        // Village area circle (lighter)
        val (villageSx, villageSy) = worldToScreen(650f, 600f)
        val villageR = 350f * zoom
        paint.color = Color.rgb(194, 178, 128); paint.style = Paint.Style.FILL
        canvas.drawCircle(villageSx, villageSy, villageR, paint)
        // Sheep pasture center
        val (pastureSx, pastureSy) = worldToScreen(1000f, 950f)
        paint.color = Color.rgb(120, 170, 80)
        canvas.drawCircle(pastureSx, pastureSy, 180f*zoom, paint)
        // Wolf forest north (dark)
        paint.color = Color.rgb(34, 60, 30)
        canvas.drawRect(0f, worldToScreen(1000f, 1700f).second - 300f*zoom, width.toFloat(), height.toFloat(), paint)
        // Trees forest hint
        paint.color = Color.rgb(20, 40, 20)
        for (i in 0..8) {
            val tx = worldToScreen(600f + i*150f, 1850f).first
            canvas.drawCircle(tx, worldToScreen(0f,1850f).second, 18f*zoom, paint)
        }

        // Grid
        val gs = 100f*zoom
        val startX = (-camX*zoom + width/2f) % gs
        val startY = (-camY*zoom + height/2f) % gs
        var x = startX; while (x < width) { canvas.drawLine(x,0f,x,height.toFloat(), gridPaint); x+=gs }
        var y = startY; while (y < height) { canvas.drawLine(0f,y,width.toFloat(),y, gridPaint); y+=gs }

        // Draw objects - special icons for گرگ‌میش
        for (obj in eng.objects) {
            val (sx, sy) = worldToScreen(obj.x, obj.y)
            val isSelected = eng.selected == obj

            // Shadow
            paint.color = Color.argb(60,0,0,0)
            canvas.drawOval(sx-20*zoom, sy+10*zoom, sx+20*zoom, sy+16*zoom, paint)

            // Determine icon and color
            when (obj.type) {
                "sheep" -> {
                    // گوسفند سفید پشمالو
                    paint.color = Color.WHITE; paint.style = Paint.Style.FILL
                    canvas.drawCircle(sx, sy, 14f*zoom, paint)
                    paint.color = Color.rgb(255, 228, 196) // face
                    canvas.drawCircle(sx, sy+3*zoom, 7f*zoom, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(sx-3*zoom, sy+1*zoom, 2f*zoom, paint)
                    canvas.drawCircle(sx+3*zoom, sy+1*zoom, 2f*zoom, paint)
                    // Wool dots
                    paint.color = Color.WHITE
                    canvas.drawCircle(sx-8*zoom, sy-6*zoom, 5f*zoom, paint)
                    canvas.drawCircle(sx+8*zoom, sy-6*zoom, 5f*zoom, paint)
                    canvas.drawCircle(sx, sy-10*zoom, 6f*zoom, paint)
                }
                "shepherd" -> {
                    paint.color = Color.rgb(139,69,19)
                    canvas.drawCircle(sx, sy, 12f*zoom, paint)
                    paint.color = Color.RED
                    canvas.drawRect(sx-8*zoom, sy-18*zoom, sx+8*zoom, sy-10*zoom, paint)
                }
                "wolf" -> {
                    paint.color = Color.rgb(90,90,90)
                    val path = Path().apply {
                        moveTo(sx, sy-14*zoom); lineTo(sx+13*zoom, sy+6*zoom)
                        lineTo(sx+6*zoom, sy+14*zoom); lineTo(sx-6*zoom, sy+14*zoom)
                        lineTo(sx-13*zoom, sy+6*zoom); close()
                    }
                    canvas.drawPath(path, paint)
                    // Eyes red
                    paint.color = Color.RED
                    canvas.drawCircle(sx-5*zoom, sy-2*zoom, 2.5f*zoom, paint)
                    canvas.drawCircle(sx+5*zoom, sy-2*zoom, 2.5f*zoom, paint)
                    // Tail
                    paint.color = Color.rgb(60,60,60)
                    canvas.drawCircle(sx, sy+14*zoom, 5f*zoom, paint)
                }
                "alpha_wolf" -> {
                    paint.color = Color.rgb(40,40,40)
                    val path = Path().apply {
                        moveTo(sx, sy-18*zoom); lineTo(sx+16*zoom, sy+8*zoom)
                        lineTo(sx+8*zoom, sy+18*zoom); lineTo(sx-8*zoom, sy+18*zoom)
                        lineTo(sx-16*zoom, sy+8*zoom); close()
                    }
                    canvas.drawPath(path, paint)
                    paint.color = Color.RED
                    canvas.drawCircle(sx-6*zoom, sy-2*zoom, 3f*zoom, paint)
                    canvas.drawCircle(sx+6*zoom, sy-2*zoom, 3f*zoom, paint)
                    // Crown
                    paint.color = Color.YELLOW
                    canvas.drawCircle(sx, sy-20*zoom, 5f*zoom, paint)
                }
                "den" -> {
                    paint.color = Color.rgb(50,30,15)
                    canvas.drawOval(sx-50*zoom, sy-30*zoom, sx+50*zoom, sy+30*zoom, paint)
                    paint.color = Color.BLACK
                    canvas.drawOval(sx-20*zoom, sy-10*zoom, sx+20*zoom, sy+15*zoom, paint)
                    paint.color = Color.RED
                    smallText.textSize = 9f*zoom
                    canvas.drawText("DEN", sx-smallText.measureText("DEN")/2, sy+4*zoom, smallText)
                }
                else -> {
                    // Buildings / units like before
                    val baseColor = when {
                        obj.owner == faction && obj.isBuilding -> Color.rgb(30,60,120)
                        obj.owner == faction && !obj.isBuilding -> when(faction){
                            "USA"->Color.rgb(25,118,210); "China"->Color.rgb(211,47,47); else->Color.rgb(56,142,60)
                        }
                        obj.isBuilding -> Color.rgb(120,30,30)
                        else -> Color.rgb(80,80,80)
                    }
                    paint.color = baseColor; paint.style = Paint.Style.FILL
                    if (obj.isBuilding) {
                        val w=60*zoom; val h=50*zoom
                        canvas.drawRect(sx-w/2, sy-h/2, sx+w/2, sy+h/2, paint)
                        paint.color = Color.argb(60,255,255,255)
                        canvas.drawRect(sx-w/2, sy-h/2, sx+w/2, sy-h/2+8*zoom, paint)
                    } else {
                        val path = Path().apply {
                            moveTo(sx, sy-16*zoom); lineTo(sx+14*zoom, sy)
                            lineTo(sx, sy+16*zoom); lineTo(sx-14*zoom, sy); close()
                        }
                        paint.color = baseColor; canvas.drawPath(path, paint)
                        paint.color = Color.rgb(40,40,40)
                        canvas.drawCircle(sx,sy,6*zoom, paint)
                    }
                }
            }

            // Selection ring
            if (isSelected) {
                paint.color = if (obj.owner==faction || obj.owner=="Sheep") Color.GREEN else Color.RED
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f*zoom
                if (obj.isBuilding || obj.type=="den") canvas.drawRect(sx-34*zoom, sy-30*zoom, sx+34*zoom, sy+30*zoom, paint)
                else canvas.drawCircle(sx,sy,22*zoom, paint)
                paint.style = Paint.Style.FILL
            }

            // HP bar
            if (obj.type!="sheep" || obj.hp < obj.maxHp) {
                val pct = obj.hp.toFloat()/obj.maxHp.toFloat().coerceAtLeast(1f)
                val bw=40*zoom; val bh=4*zoom; val bx=sx-bw/2; val by=sy-(if(obj.isBuilding||obj.type=="den")32 else 26)*zoom
                paint.color = Color.BLACK; paint.style= Paint.Style.FILL
                canvas.drawRect(bx,by,bx+bw,by+bh, paint)
                paint.color = when { pct>0.6->Color.GREEN; pct>0.3->Color.YELLOW; else->Color.RED }
                canvas.drawRect(bx,by,bx+bw*pct.coerceIn(0f,1f),by+bh, paint)
            }

            // Name
            if (obj.type=="sheep" || obj.type=="wolf" || obj.type=="alpha_wolf") {
                // smaller
                smallText.textSize = 8f*zoom.coerceAtLeast(0.8f)
                smallText.color = Color.WHITE
                smallText.setShadowLayer(2f,1f,1f, Color.BLACK)
                val label = if(obj.type=="alpha_wolf") "گرگ آلفا" else if(obj.type=="wolf") "گرگ" else "🐑"
                if (obj.type=="sheep") {
                    canvas.drawText(label, sx-smallText.measureText(label)/2, sy+26*zoom, smallText)
                } else {
                    canvas.drawText(obj.name, sx-smallText.measureText(obj.name)/2, sy+30*zoom, smallText)
                }
                smallText.clearShadowLayer()
            } else {
                smallText.textSize = 10f*zoom.coerceAtLeast(0.8f); smallText.color=Color.WHITE
                smallText.setShadowLayer(2f,1f,1f, Color.BLACK)
                canvas.drawText(obj.name, sx-smallText.measureText(obj.name)/2, sy+30*zoom, smallText)
                smallText.clearShadowLayer()
            }

            // Attack line
            if (obj.state=="attacking" && obj.target!=null) {
                paint.color = Color.RED; paint.strokeWidth=2f; paint.style= Paint.Style.STROKE
                val (tx,ty)=worldToScreen(obj.target!!.x, obj.target!!.y)
                canvas.drawLine(sx,sy,tx,ty, paint)
                paint.style= Paint.Style.FILL
                canvas.drawCircle(tx,ty,4*zoom, paint)
            }
        }

        // HUD overlay for گرگ‌میش
        // Top info
        textPaint.textSize = 11f; textPaint.color = Color.YELLOW
        canvas.drawText("گرگ‌میش • گوسفندان: ${eng.sheepAlive()}/${eng.sheepTotal()}  گرگ‌ها: ${eng.wolvesAlive()}", 12f, 22f, textPaint)

        textPaint.textSize = 10f; textPaint.color = Color.argb(180,255,255,255)
        val modeText = if (eng.isPaused) "PAUSED" else "Tap=انتخاب  LongPress=حرکت/حمله  Pinch=زوم  Drag=جابجایی"
        canvas.drawText(modeText, 12f, height-90f, textPaint)

        textPaint.color = when(faction){ "USA"->Color.rgb(25,118,210); "China"->Color.rgb(211,47,47); else->Color.rgb(56,142,60) }
        textPaint.textSize = 10f
        canvas.drawText("فرمانده: $faction  •  ${eng.objects.size} واحد", width-300f, 50f, textPaint)

        if (eng.selected==null) {
            textPaint.color = Color.YELLOW; textPaint.textSize=13f
            val h="یک واحد را لمس کن - از گله محافظت کن!"
            canvas.drawText(h, width/2f - textPaint.measureText(h)/2, 60f, textPaint)
        }

        // Village label
        val (vx, vy) = worldToScreen(650f, 600f)
        smallText.textSize=11f*zoom; smallText.color=Color.rgb(60,40,10)
        canvas.drawText("روستا", vx-smallText.measureText("روستا")/2, vy-45*zoom, smallText)
        val (px, py) = worldToScreen(1000f, 950f)
        smallText.color=Color.rgb(34,100,34)
        canvas.drawText("مرتع گوسفندان", px-smallText.measureText("مرتع گوسفندان")/2, py-100*zoom, smallText)
        val (wx, wy) = worldToScreen(1000f, 1950f)
        smallText.color=Color.RED
        canvas.drawText("لانه گرگ‌ها", wx-smallText.measureText("لانه گرگ‌ها")/2, wy-45*zoom, smallText)
    }
}
