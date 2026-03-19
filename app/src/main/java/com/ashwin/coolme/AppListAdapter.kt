package com.ashwin.coolme

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(private val appList: List<AppInfo>) :
    RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val appBatteryUsage: TextView = view.findViewById(R.id.appBatteryUsage)
        val appCheckbox: CheckBox = view.findViewById(R.id.appCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = appList[position]
        holder.appName.text = appInfo.name
        holder.appIcon.setImageDrawable(appInfo.icon)
        
        // Show simulated battery usage
        val batteryText = String.format("Power consuming: %.1f%%", appInfo.batteryPercentage)
        holder.appBatteryUsage.text = batteryText
        
        // Disable listener temporarily to prevent unwanted triggers during scrolling
        holder.appCheckbox.setOnCheckedChangeListener(null)
        holder.appCheckbox.isChecked = appInfo.isSelected

        holder.itemView.setOnClickListener {
            appInfo.isSelected = !appInfo.isSelected
            holder.appCheckbox.isChecked = appInfo.isSelected
        }
        
        holder.appCheckbox.setOnCheckedChangeListener { _, isChecked ->
            appInfo.isSelected = isChecked
        }
    }

    override fun getItemCount() = appList.size
    
    fun getSelectedApps(): List<AppInfo> {
        return appList.filter { it.isSelected }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(selectAll: Boolean) {
        for (app in appList) {
            app.isSelected = selectAll
        }
        notifyDataSetChanged()
    }
}