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
        var appsToClose = mutableListOf<String>()
        var lastPackageHandled: String? = null
        
        fun start(packages: List<String>) {
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
        info.flags = AccessibilityServiceInfo.DEFAULT or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        this.serviceInfo = info
        Log.d("AppCooler", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRunning || appsToClose.isEmpty()) return

        val rootNode = rootInActiveWindow ?: return
        val currentPackage = appsToClose[0]
        
        // Try finding by common IDs first (more reliable across languages)
        var forceStopNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button")
        
        // Fallback to text search for "Force stop" or localized equivalents
        if (forceStopNodes.isEmpty()) {
            forceStopNodes = rootNode.findAccessibilityNodeInfosByText("Force stop")
        }
        if (forceStopNodes.isEmpty()) {
            forceStopNodes = rootNode.findAccessibilityNodeInfosByText("Force Stop")
        }

        if (forceStopNodes.isNotEmpty()) {
            val button = forceStopNodes[0]
            if (button.isEnabled) {
                if (button.isClickable) {
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    // Sometimes the button itself isn't clickable but its parent is
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
                // Ensure we only skip it once.
                if (lastPackageHandled != currentPackage) {
                    lastPackageHandled = currentPackage
                    Log.d("AppCooler", "App $currentPackage already stopped or cannot be stopped. Skipping.")
                    if (appsToClose.isNotEmpty()) {
                        appsToClose.removeAt(0)
                    }
                    closeNextApp()
                }
            }
        } else {
            // Check for confirmation dialog buttons
            // Common IDs: android:id/button1 (usually OK/Positive)
            var okNodes = rootNode.findAccessibilityNodeInfosByViewId("android:id/button1")
            
            if (okNodes.isEmpty()) {
                okNodes = rootNode.findAccessibilityNodeInfosByText("OK")
            }
            if (okNodes.isEmpty()) {
                okNodes = rootNode.findAccessibilityNodeInfosByText("Force stop") // In some dialogs the button is also "Force stop"
            }
            if (okNodes.isEmpty()) {
                okNodes = rootNode.findAccessibilityNodeInfosByText("Force Stop")
            }

            if (okNodes.isNotEmpty()) {
                val okButton = okNodes[0]
                if (okButton.isEnabled && okButton.isClickable) {
                    // Only click OK if we haven't already handled this package's closing
                    if (lastPackageHandled != currentPackage) {
                        lastPackageHandled = currentPackage
                        okButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        // Once closed, move to the next app
                        if (appsToClose.isNotEmpty()) {
                            appsToClose.removeAt(0)
                        }
                        closeNextApp()
                    }
                }
            }
        }
    }

    fun startClosingApps(packages: List<String>) {
        appsToClose.clear()
        appsToClose.addAll(packages)
        isRunning = true
        lastPackageHandled = null
        closeNextApp()
    }

    private fun closeNextApp() {
        if (appsToClose.isNotEmpty()) {
            val packageName = appsToClose[0]
            // If the next app is already the one we just handled (shouldn't happen), skip it
            if (packageName == lastPackageHandled) {
                appsToClose.removeAt(0)
                closeNextApp()
                return
            }
            
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(intent)
        } else {
            isRunning = false
            lastPackageHandled = null
            // Go back to the main app when done
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
