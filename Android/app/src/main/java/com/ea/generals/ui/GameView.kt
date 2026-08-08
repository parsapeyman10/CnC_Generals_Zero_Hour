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
import kotlin.math.max
import kotlin.math.min

/**
 * GameView - 2D RTS view that preserves PC's W3DView behavior
 * This is a Canvas implementation for A30 (Mali-G71) - fast and battery friendly
 * Future can be replaced with GLES rendering of actual W3D meshes
 *
 * Touch mapping (preserves PC mouse):
 *  Tap -> Select (Left Click)
 *  Long Press -> Move/Attack (Right Click)
 *  Drag -> Scroll map (Middle Drag)
 *  Pinch -> Zoom (Wheel)
 */
class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var engine: GeneralsEngine? = null
    private var faction: String = "USA"

    var onCreditsChanged: ((Int) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    // Camera like PC's W3DView
    private var camX = 1000f
    private var camY = 1000f
    private var zoom = 1f // 0.5 .. 2.0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = Typeface.MONOSPACE
    }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f
        typeface = Typeface.MONOSPACE
    }

    private val terrainPaint = Paint().apply { color = Color.rgb(194, 178, 128) }
    private val gridPaint = Paint().apply {
        color = Color.argb(30, 0, 0, 0)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var gestureDetector: GestureDetector
    private var scaleDetector: ScaleGestureDetector
    private var isLongPress = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleTap(e.x, e.y, false)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                isLongPress = true
                handleTap(e.x, e.y, true)
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                // Drag to scroll map (like PC middle drag)
                camX += distanceX / zoom
                camY += distanceY / zoom
                clampCamera()
                invalidate()
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                zoom = (zoom * factor).coerceIn(0.6f, 2.5f)
                invalidate()
                return true
            }
        })

        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isLongPress = false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isLongPress && event.eventTime - event.downTime < 200) {
                        // Already handled as singleTap
                    }
                }
            }
            true
        }
    }

    fun setEngine(e: GeneralsEngine) {
        engine = e
    }

    fun setFaction(f: String) {
        faction = f
    }

    fun tick() {
        invalidate()
    }

    fun pause() {}
    fun resume() {}

    private fun handleTap(screenX: Float, screenY: Float, longPress: Boolean) {
        val worldX = (screenX / zoom) + camX - (width / 2f / zoom)
        val worldY = (screenY / zoom) + camY - (height / 2f / zoom)
        engine?.onWorldTapped(worldX, worldY, longPress)
        invalidate()
    }

    private fun clampCamera() {
        camX = camX.coerceIn(400f, 1600f)
        camY = camY.coerceIn(400f, 1600f)
    }

    private fun worldToScreen(wx: Float, wy: Float): Pair<Float, Float> {
        val sx = (wx - camX) * zoom + width / 2f
        val sy = (wy - camY) * zoom + height / 2f
        return sx to sy
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        // Draw terrain with grid (like PC)
        canvas.drawColor(Color.rgb(194, 178, 128))
        // Grid
        val gridSize = 100f * zoom
        val startX = (-camX * zoom + width / 2f) % gridSize
        val startY = (-camY * zoom + height / 2f) % gridSize
        var x = startX
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += gridSize
        }
        var y = startY
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += gridSize
        }

        // Draw objects
        for (obj in eng.objects) {
            val (sx, sy) = worldToScreen(obj.x, obj.y)
            val isSelected = eng.selected == obj
            val isEnemy = obj.owner != faction && obj.owner != "Enemy" // simplified

            // Shadow
            paint.color = Color.argb(60, 0, 0, 0)
            canvas.drawOval(sx - 20 * zoom, sy + 10 * zoom, sx + 20 * zoom, sy + 16 * zoom, paint)

            // Body color by faction like PC
            val baseColor = when {
                obj.owner == faction && obj.isBuilding -> Color.rgb(30, 60, 120)
                obj.owner == faction && !obj.isBuilding -> when (faction) {
                    "USA" -> Color.rgb(25, 118, 210)
                    "China" -> Color.rgb(211, 47, 47)
                    else -> Color.rgb(56, 142, 60)
                }
                obj.isBuilding -> Color.rgb(120, 30, 30)
                else -> Color.rgb(80, 80, 80)
            }
            paint.color = baseColor
            paint.style = Paint.Style.FILL

            if (obj.isBuilding) {
                val w = 60 * zoom
                val h = 50 * zoom
                canvas.drawRect(sx - w / 2, sy - h / 2, sx + w / 2, sy + h / 2, paint)
                // Roof
                paint.color = Color.argb(60, 255, 255, 255)
                canvas.drawRect(sx - w / 2, sy - h / 2, sx + w / 2, sy - h / 2 + 8 * zoom, paint)
                paint.color = baseColor
            } else {
                // Unit - diamond shape like PC
                val path = Path().apply {
                    moveTo(sx, sy - 16 * zoom)
                    lineTo(sx + 14 * zoom, sy)
                    lineTo(sx, sy + 16 * zoom)
                    lineTo(sx - 14 * zoom, sy)
                    close()
                }
                canvas.drawPath(path, paint)
                // Turret
                paint.color = Color.rgb(40, 40, 40)
                canvas.drawCircle(sx, sy, 6 * zoom, paint)
            }

            // Selection ring (like PC green/red)
            if (isSelected) {
                paint.color = if (obj.owner == faction) Color.GREEN else Color.RED
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f * zoom
                if (obj.isBuilding) {
                    canvas.drawRect(sx - 34 * zoom, sy - 30 * zoom, sx + 34 * zoom, sy + 30 * zoom, paint)
                } else {
                    canvas.drawCircle(sx, sy, 22 * zoom, paint)
                }
                paint.style = Paint.Style.FILL
            }

            // HP bar (like PC)
            val hpPct = obj.hp.toFloat() / obj.maxHp.toFloat()
            val barW = 40 * zoom
            val barH = 4 * zoom
            val barX = sx - barW / 2
            val barY = sy - (if (obj.isBuilding) 32 else 26) * zoom
            paint.color = Color.BLACK
            canvas.drawRect(barX, barY, barX + barW, barY + barH, paint)
            paint.color = when {
                hpPct > 0.6 -> Color.GREEN
                hpPct > 0.3 -> Color.YELLOW
                else -> Color.RED
            }
            canvas.drawRect(barX, barY, barX + barW * hpPct, barY + barH, paint)

            // Name
            smallText.textSize = 10f * zoom.coerceAtLeast(0.8f)
            smallText.color = Color.WHITE
            // Shadow
            smallText.setShadowLayer(2f, 1f, 1f, Color.BLACK)
            canvas.drawText(obj.name, sx - smallText.measureText(obj.name) / 2, sy + 30 * zoom, smallText)
            smallText.clearShadowLayer()

            // State indicator
            if (obj.state == "attacking" && obj.target != null) {
                paint.color = Color.RED
                paint.strokeWidth = 2f
                val (tx, ty) = worldToScreen(obj.target!!.x, obj.target!!.y)
                canvas.drawLine(sx, sy, tx, ty, paint)
                // Red dot at target
                paint.style = Paint.Style.FILL
                canvas.drawCircle(tx, ty, 4 * zoom, paint)
            }
        }

        // Draw mode indicator
        textPaint.textSize = 12f
        textPaint.color = Color.argb(180, 255, 255, 255)
        val modeText = when (eng.isPaused) {
            true -> "PAUSED - Tap Menu to resume"
            false -> "Mode: Tap=Select  LongPress=Move/Attack  Pinch=Zoom"
        }
        canvas.drawText(modeText, 12f, height - 90f, textPaint)

        // Faction banner
        textPaint.color = when (faction) {
            "USA" -> Color.rgb(25, 118, 210)
            "China" -> Color.rgb(211, 47, 47)
            else -> Color.rgb(56, 142, 60)
        }
        textPaint.textSize = 10f
        canvas.drawText("COMMANDER: $faction  •  ${eng.objects.size} OBJECTS", width - 260f, 50f, textPaint)

        // If no selection hint
        if (eng.selected == null) {
            textPaint.color = Color.YELLOW
            textPaint.textSize = 14f
            val hint = "Tap a unit to select"
            canvas.drawText(hint, width / 2f - textPaint.measureText(hint) / 2, 60f, textPaint)
        }
    }
}
