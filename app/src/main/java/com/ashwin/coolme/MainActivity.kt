package com.ashwin.coolme

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.util.Log
import com.google.android.material.chip.Chip
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private var appList = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewApps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val btnCoolDown: Button = findViewById(R.id.btnCoolDown)
        val cbSelectAll: CheckBox = findViewById(R.id.cbSelectAll)
        val searchView: SearchView = findViewById(R.id.searchView)
        
        val chipPower: Chip = findViewById(R.id.chipPower)
        val chipRam: Chip = findViewById(R.id.chipRam)
        val chipStorage: Chip = findViewById(R.id.chipStorage)

        // Initialize adapter with empty list initially
        adapter = AppListAdapter(appList) { appInfo ->
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${appInfo.packageName}")
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadApps()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (::adapter.isInitialized) {
                adapter.selectAll(isChecked)
            }
        }
        
        chipPower.setOnClickListener { adapter.sortByPower() }
        chipRam.setOnClickListener { adapter.sortByRam() }
        chipStorage.setOnClickListener { adapter.sortByStorage() }

        btnCoolDown.setOnClickListener {
            if (!isAccessibilityServiceEnabled(this, ForceStopAccessibilityService::class.java)) {
                showAccessibilityDialog()
                return@setOnClickListener
            }

            val selectedApps = adapter.getSelectedApps()
            if (selectedApps.isEmpty()) {
                Toast.makeText(this, "Please select at least one app to close", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val appData = selectedApps.map { it.packageName to it.name }
            
            ForceStopAccessibilityService.start(appData)
            
            if (ForceStopAccessibilityService.appsToClose.isNotEmpty()) {
               val (pName, _) = ForceStopAccessibilityService.appsToClose[0]
               val detailIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$pName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
               }
               startActivity(detailIntent)
            }
            
            Toast.makeText(this, "Cooling down... Closing selected apps", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh apps list in case some were uninstalled
        if (::adapter.isInitialized) {
            loadApps()
        }
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val tempList = mutableListOf<AppInfo>()
            
            for (packageInfo in packages) {
                try {
                    val isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    if ((!isSystemApp || isUpdatedSystemApp) && packageInfo.packageName != packageName) {
                        val name = pm.getApplicationLabel(packageInfo).toString()
                        val icon = pm.getApplicationIcon(packageInfo)
                        val packageName = packageInfo.packageName
                        
                        // Mocking percentage values for demo purposes
                        // Real values for per-app battery and RAM are heavily restricted on modern Android
                        val powerPct = Random.nextFloat() * 20.0f
                        val ramPct = Random.nextFloat() * 15.0f
                        val storagePct = Random.nextFloat() * 10.0f
                        
                        tempList.add(AppInfo(name, packageName, icon, powerPct, ramPct, storagePct))
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading app: ${packageInfo.packageName}", e)
                }
            }
            
            tempList.sortByDescending { it.batteryPercentage }
            
            runOnUiThread {
                appList.clear()
                appList.addAll(tempList)
                adapter.updateFullList(tempList)
                if (appList.isEmpty()) {
                   Toast.makeText(this@MainActivity, "No user apps found to display.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(context.packageName + "/" + service.name, ignoreCase = true)
                    || componentName.equals(context.packageName + "/" + service.canonicalName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To automatically close apps and cool down your phone, please enable the Coolme Accessibility Service in settings.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}