package com.ashwin.coolme

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val batteryPercentage: Float, // Mock value since system API is restricted
    var isSelected: Boolean = false
)