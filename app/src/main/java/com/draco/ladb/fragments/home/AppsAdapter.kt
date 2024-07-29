package com.draco.ladb.fragments.home

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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

    // This will hold the filtered list of apps
    private var filteredAppsList: List<AppInfo> = appsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = filteredAppsList[position]
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

    // Method to update the list of apps
    fun updateList(newList: List<AppInfo>) {
        appsList = newList
        filteredAppsList = newList
        notifyDataSetChanged()
    }

    // Method to filter the list of apps based on the search query
    fun filter(query: String?) {
        filteredAppsList = if (query.isNullOrEmpty()) {
            appsList
        } else {
            appsList.filter {
                it.appName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    private fun showConfirmationDialog(context: Context, packageName: String, isSystemApp: Boolean) {
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
        return filteredAppsList.size
    }

    /** To be used when phone is not listening on port 5555 */
    private fun uninstallOrDisableApp(packageName: String, isSystemApp: Boolean) {
        if (isSystemApp) {
            try {
                Log.d("AppsAdapter", "Attempting to disable app: $packageName")
                val disableIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (disableIntent.resolveActivity(packageManager) != null) {
                    ContextCompat.startActivity(context, disableIntent, null)
                } else {
                    Log.e("AppsAdapter", "Disable intent could not be resolved for: $packageName")
                }
            } catch (e: Exception) {
                Log.e("AppsAdapter", "Disable failed for: $packageName", e)
                e.printStackTrace()
            }
        } else {
            try {
                Log.d("AppsAdapter", "Attempting to uninstall app: $packageName")
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ContextCompat.startActivity(context, uninstallIntent, null)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("AppsAdapter", "Refreshing app list after uninstallation attempt")
                    refreshCallback()
                }, 2000) // Adjust the delay as needed
            } catch (e: Exception) {
                Log.e("AppsAdapter", "Uninstall failed for: $packageName", e)

            }
        }



    }

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appTitle: TextView = itemView.findViewById(R.id.app_title)
        val appPackageName: TextView = itemView.findViewById(R.id.app_package_name)
        val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        val trashIcon: ImageView = itemView.findViewById(R.id.trash_icon)
    }
}
