package com.ashwin.coolme

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val batteryPercentage: Float,
    val ramUsage: Float,
    val storageUsage: Float,
    var isSelected: Boolean = false
)