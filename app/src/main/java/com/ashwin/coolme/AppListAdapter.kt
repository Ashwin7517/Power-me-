package com.ashwin.coolme

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AppListAdapter(private var appList: List<AppInfo>) :
    RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var fullList: List<AppInfo> = ArrayList(appList)

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val appBatteryUsage: TextView = view.findViewById(R.id.appBatteryUsage)
        val appRamUsage: TextView = view.findViewById(R.id.appRamUsage)
        val appStorageUsage: TextView = view.findViewById(R.id.appStorageUsage)
        val appCheckbox: CheckBox = view.findViewById(R.id.appCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = appList[position]
        holder.appName.text = appInfo.name
        holder.appIcon.setImageDrawable(appInfo.icon)
        
        holder.appBatteryUsage.text = String.format("Power: %.1f%%", appInfo.batteryPercentage)
        holder.appRamUsage.text = String.format("RAM: %.1f%%", appInfo.ramUsage)
        holder.appStorageUsage.text = String.format("Storage: %.1f%%", appInfo.storageUsage)
        
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
        return fullList.filter { it.isSelected }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    fun sortByPower() {
        appList = appList.sortedByDescending { it.batteryPercentage }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortByRam() {
        appList = appList.sortedByDescending { it.ramUsage }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortByStorage() {
        appList = appList.sortedByDescending { it.storageUsage }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(selectAll: Boolean) {
        for (app in appList) {
            app.isSelected = selectAll
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFullList(newList: List<AppInfo>) {
        this.fullList = ArrayList(newList)
        this.appList = ArrayList(newList)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        appList = if (query.isEmpty()) {
            ArrayList(fullList)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            fullList.filter { it.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) }
        }
        notifyDataSetChanged()
    }
}
