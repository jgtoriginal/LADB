package com.draco.ladb.fragments.home

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.draco.ladb.R

data class AppInfo(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable,
    val isSystemApp: Boolean
)

class AppsAdapter(
    private val context: Context,
    private var appsList: List<AppInfo>,
    private val packageManager: PackageManager,
    private val deleteApp: (input: String) -> Unit,
    private val refreshCallback: () -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = appsList[position]
        val appTitle = if (appInfo.isSystemApp) {
            "${appInfo.appName} - SYSTEM"
        } else {
            appInfo.appName
        }
        holder.appTitle.text = appTitle
        holder.appPackageName.text = appInfo.packageName
        holder.appIcon.setImageDrawable(appInfo.appIcon)

        holder.trashIcon.setOnClickListener {
            showConfirmationDialog(context, appInfo.packageName, appInfo.isSystemApp)
        }
    }


    // Assuming this is within an Activity or a Fragment
    fun showConfirmationDialog(context: Context, packageName: String, isSystemApp: Boolean) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Action")
        builder.setMessage("Are you sure you want to delete this app?")

        // Positive button
        builder.setPositiveButton("Yes") { dialog: DialogInterface, _: Int ->
            deleteApp(packageName)
            refreshCallback()
        }

        // Negative button
        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()  // Just close the dialog if user selects No
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    override fun getItemCount(): Int {
        return appsList.size
    }

    fun updateList(newList: List<AppInfo>) {
        appsList = newList
        notifyDataSetChanged()
    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appTitle: TextView = itemView.findViewById(R.id.app_title)
        val appPackageName: TextView = itemView.findViewById(R.id.app_package_name)
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val trashIcon: ImageView = itemView.findViewById(R.id.trash_icon)
    }
}
