package com.ashwin.coolme

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ForceStopAccessibilityService : AccessibilityService() {

    companion object {
        var isRunning = false
        var appsToClose = mutableListOf<Pair<String, String>>()
        var lastPackageHandled: String? = null
        
        fun start(packages: List<Pair<String, String>>) {
            appsToClose.clear()
            appsToClose.addAll(packages)
            lastPackageHandled = null
            isRunning = true
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        this.serviceInfo = info
        Log.d("AppCooler", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRunning || appsToClose.isEmpty()) return

        val rootNode = rootInActiveWindow ?: return
        val (currentPackage, currentAppName) = appsToClose[0]
        
        // Ensure we are in Settings or system dialog
        val eventPackage = event.packageName?.toString() ?: ""
        if (eventPackage != "com.android.settings" && eventPackage != "android" && eventPackage != "com.google.android.settings") {
            return
        }

        // Verify we are looking at the correct app's info page
        // We look for the app name in the window to avoid racing conditions from previous apps
        val appNameNodes = rootNode.findAccessibilityNodeInfosByText(currentAppName)
        val isCorrectPage = appNameNodes.isNotEmpty()
        
        // Try finding "Force stop" button by common IDs
        var forceStopNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")
        if (forceStopNodes.isEmpty()) {
            forceStopNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.settings:id/right_button")
        }
        
        // Fallback to localized text search for "Force stop"
        if (forceStopNodes.isEmpty()) {
            val forceStopTexts = listOf(
                "Force stop", "Force Stop", "FORCE STOP", 
                "Forzar detención", "Forzar Detención", 
                "Forcer l'arrêt", "Forcer l’arrêt",
                "Beenden erzwingen", "Termina", "Stoppen"
            )
            for (text in forceStopTexts) {
                forceStopNodes = rootNode.findAccessibilityNodeInfosByText(text)
                if (forceStopNodes.isNotEmpty()) break
            }
        }

        if (forceStopNodes.isNotEmpty() && isCorrectPage) {
            val button = forceStopNodes[0]
            if (button.isEnabled) {
                if (button.isClickable) {
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    var parent = button.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            break
                        }
                        parent = parent.parent
                    }
                }
            } else {
                // Button is disabled, app is already stopped or cannot be stopped.
                if (lastPackageHandled != currentPackage) {
                    lastPackageHandled = currentPackage
                    Log.d("AppCooler", "App $currentPackage already stopped or cannot be stopped.")
                    if (appsToClose.isNotEmpty()) {
                        appsToClose.removeAt(0)
                    }
                    closeNextApp()
                }
            }
        } else {
            // Check for confirmation dialog buttons
            var okNodes = rootNode.findAccessibilityNodeInfosByViewId("android:id/button1")
            
            if (okNodes.isEmpty()) {
                val okTexts = listOf("OK", "Ok", "ok", "Forzar detención", "Force stop", "Force Stop", "TERMINA", "STOPPEN")
                for (text in okTexts) {
                    okNodes = rootNode.findAccessibilityNodeInfosByText(text)
                    if (okNodes.isNotEmpty()) break
                }
            }

            if (okNodes.isNotEmpty()) {
                val okButton = okNodes[0]
                if (okButton.isEnabled && okButton.isClickable) {
                    if (lastPackageHandled != currentPackage) {
                        lastPackageHandled = currentPackage
                        okButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (appsToClose.isNotEmpty()) {
                            appsToClose.removeAt(0)
                        }
                        closeNextApp()
                    }
                }
            }
        }
    }

    private fun closeNextApp() {
        if (appsToClose.isNotEmpty()) {
            val (packageName, _) = appsToClose[0]
            if (packageName == lastPackageHandled) {
                appsToClose.removeAt(0)
                closeNextApp()
                return
            }
            
            // Add a small delay to allow UI to settle before starting next activity
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AppCooler", "Error starting settings for $packageName", e)
                    if (appsToClose.isNotEmpty()) {
                        appsToClose.removeAt(0)
                    }
                    closeNextApp()
                }
            }, 300)
        } else {
            isRunning = false
            lastPackageHandled = null
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        isRunning = false
    }
}