package com.example.divineroster.ui.dashboard

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.divineroster.R

fun TextView.bindUrgencyBadge(urgencyLevel: String) {
    val (label, background, textColor) = when (urgencyLevel) {
        "Urgent" -> Triple("URGENT", R.drawable.bg_badge_urgent, R.color.badge_urgent_txt)
        "High Importance" -> Triple("HIGH IMPORTANCE", R.drawable.bg_badge_high, R.color.badge_high_txt)
        else -> Triple("NORMAL", R.drawable.bg_badge_normal, R.color.badge_normal_txt)
    }
    text = label
    setBackgroundResource(background)
    setTextColor(ContextCompat.getColor(context, textColor))
}
