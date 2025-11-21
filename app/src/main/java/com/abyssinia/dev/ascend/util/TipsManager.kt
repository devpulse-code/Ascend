package com.abyssinia.dev.ascend.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.abyssinia.dev.ascend.R
import android.animation.ValueAnimator
class TipsManager(private val activity: Activity) {

    private val sharedPref: SharedPreferences = activity.getSharedPreferences("tips_pref", Context.MODE_PRIVATE)
    private var tipIndex = 0
    private lateinit var tipsList: List<TipItem>

    data class TipItem(
        val targetView: View,
        val message: String,
        val isFab: Boolean
    )

    fun showTipsIfNeeded(targets: List<TipItem>) {
        val lastShown = sharedPref.getLong("last_shown", 0)
        val now = System.currentTimeMillis()

        // Show if never shown or a week has passed
        if (now - lastShown > 7 * 24 * 60 * 60 * 1000 || lastShown == 0L) {
            tipsList = targets
            tipIndex = 0
            showNextTip()
        }
    }

    private fun showNextTip() {
        if (tipIndex >= tipsList.size) {
            sharedPref.edit().putLong("last_shown", System.currentTimeMillis()).apply()
            return
        }

        val tip = tipsList[tipIndex]

        if (tip.isFab) {
            showFabTip(tip)
        } else {
            showCardTip(tip)
        }
    }

    private fun showFabTip(tip: TipItem) {
        val overlay = FrameLayout(activity)
        overlay.setBackgroundColor(Color.parseColor("#88000000"))

        val bubble = TextView(activity)
        bubble.text = tip.message
        bubble.setTextColor(Color.WHITE)
        bubble.gravity = Gravity.CENTER
        bubble.setBackgroundResource(R.drawable.circle_background)

        val size = tip.targetView.width.coerceAtLeast(150)
        val params = FrameLayout.LayoutParams(size, size)
        val location = IntArray(2)
        tip.targetView.getLocationOnScreen(location)
        params.leftMargin = location[0]
        params.topMargin = location[1]

        overlay.addView(bubble, params)

        addOverlayToRoot(overlay)

        bubble.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                removeOverlay(overlay)
                tipIndex++
                showNextTip()
            }
            true
        }
    }

    private fun showCardTip(tip: TipItem) {
        val overlay = FrameLayout(activity)
        overlay.setBackgroundColor(Color.parseColor("#00000000")) // Transparent overlay for pulsing

        val location = IntArray(2)
        tip.targetView.getLocationOnScreen(location)
        val width = tip.targetView.width
        val height = tip.targetView.height

        // Pulse animation
        // Pulse animation
        val pulse = ObjectAnimator.ofFloat(tip.targetView, "scaleX", 1f, 1.05f, 1f)
        val pulseY = ObjectAnimator.ofFloat(tip.targetView, "scaleY", 1f, 1.05f, 1f)
        val animSet = AnimatorSet().apply {
            playTogether(pulse, pulseY)
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulse.repeatCount = ValueAnimator.INFINITE
        pulseY.repeatCount = ValueAnimator.INFINITE
        animSet.start()

        // Text below card
        val textView = TextView(activity)
        textView.text = tip.message
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        val textParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textParams.topMargin = location[1] + height + 16
        textParams.leftMargin = location[0] + width / 4
        overlay.addView(textView, textParams)

        addOverlayToRoot(overlay)

        tip.targetView.setOnClickListener {
            animSet.cancel()
            removeOverlay(overlay)
            tipIndex++
            showNextTip()
        }
    }

    private fun addOverlayToRoot(overlay: View) {
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        root.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun removeOverlay(overlay: View) {
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        root.removeView(overlay)
    }
}
