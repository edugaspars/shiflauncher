package io.github.edugaspars.shiflauncher.gestures

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Recria os gestos de navegação em tela cheia do HyperOS 3 com janelas
 * invisíveis nas bordas (o mesmo princípio de apps como o Fluid Navigation Gestures):
 *
 *  - deslizar da borda inferior para cima ............ Início
 *  - deslizar para cima e segurar ..................... Recentes
 *  - deslizar para os lados na borda inferior ......... App anterior
 *  - deslizar de qualquer lateral para dentro ......... Voltar
 */
class NavigationGestures(private val service: AccessibilityService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val density = service.resources.displayMetrics.density
    private val views = mutableListOf<View>()
    private var showLine = true

    private fun dp(v: Float) = v * density

    fun attach(showLine: Boolean) {
        detach()
        this.showLine = showLine
        val metrics = service.resources.displayMetrics
        val bottomHeight = dp(22f).toInt()
        val sideWidth = dp(18f).toInt()
        // Laterais: dos ~20% de cima até logo acima da área inferior, como no HyperOS.
        val sideHeight = (metrics.heightPixels * 0.78f).toInt() - bottomHeight

        add(BottomView(service), WindowManager.LayoutParams.MATCH_PARENT, bottomHeight, Gravity.BOTTOM, 0)
        add(SideView(service, left = true), sideWidth, sideHeight, Gravity.BOTTOM or Gravity.START, bottomHeight)
        add(SideView(service, left = false), sideWidth, sideHeight, Gravity.BOTTOM or Gravity.END, bottomHeight)
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null)
        views.forEach { runCatching { wm.removeView(it) } }
        views.clear()
    }

    private fun add(view: View, width: Int, height: Int, gravity: Int, y: Int) {
        val lp = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            this.y = y
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        runCatching { wm.addView(view, lp); views += view }
    }

    private fun action(view: View, action: Int) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        service.performGlobalAction(action)
    }

    /** Troca para o app anterior: "dois toques em recentes", como o botão faz. */
    private fun previousApp(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        handler.postDelayed({ service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) }, 120)
    }

    @SuppressLint("ViewConstructor", "ClickableViewAccessibility")
    private inner class BottomView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 210 }
        private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; alpha = 60 }
        private val line = RectF()

        private var startX = 0f
        private var startY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var handled = false
        private val upThreshold = dp(48f)
        private val sideThreshold = dp(64f)
        private val holdMove = dp(10f)
        private val recents = Runnable {
            if (!handled) {
                handled = true
                action(this, AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (!showLine) return
            val w = dp(108f)
            val h = dp(4f)
            val cx = width / 2f
            val cy = height - dp(8f)
            line.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
            canvas.drawRoundRect(RectF(line).apply { inset(-dp(0.5f), -dp(0.5f)) }, h, h, shadow)
            canvas.drawRoundRect(line, h, h, paint)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX; startY = e.rawY; lastX = startX; lastY = startY
                    handled = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = startY - e.rawY
                    if (hypot(e.rawX - lastX, e.rawY - lastY) > holdMove) {
                        lastX = e.rawX; lastY = e.rawY
                        handler.removeCallbacks(recents)
                        // Subiu o suficiente: se parar o dedo aqui, abre os recentes.
                        if (dy > upThreshold * 1.6f) handler.postDelayed(recents, 220)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(recents)
                    if (handled) return true
                    val dy = startY - e.rawY
                    val dx = e.rawX - startX
                    when {
                        dy > upThreshold && dy > abs(dx) * 0.6f ->
                            action(this, AccessibilityService.GLOBAL_ACTION_HOME)
                        abs(dx) > sideThreshold -> previousApp(this)
                    }
                    handled = true
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(recents)
            }
            return true
        }
    }

    @SuppressLint("ViewConstructor", "ClickableViewAccessibility")
    private inner class SideView(context: Context, private val left: Boolean) : View(context) {
        private var startX = 0f
        private var startY = 0f
        private var armed = false
        private val threshold = dp(56f)

        override fun onTouchEvent(e: MotionEvent): Boolean {
            val inward = if (left) e.rawX - startX else startX - e.rawX
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX; startY = e.rawY; armed = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowArmed = inward > threshold && abs(e.rawY - startY) < inward * 1.5f
                    if (nowArmed != armed) {
                        armed = nowArmed
                        if (armed) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (armed) action(this, AccessibilityService.GLOBAL_ACTION_BACK)
                    armed = false
                }
                MotionEvent.ACTION_CANCEL -> armed = false
            }
            return true
        }
    }
}
