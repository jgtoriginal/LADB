package com.draco.ladb.views

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.draco.ladb.R
import com.draco.ladb.viewmodels.MainActivityViewModel

data class AppRow(
    val title:String,
    val description:String,
    val imageId:Drawable
)
class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var list: MutableList<PackageInfo>
    private lateinit var myListAdapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        title = "Installed Apps"
        listView = findViewById(R.id.listView)
        list = packageManager.getInstalledPackages(0)
        installedApps(list)
    }

    fun deleteApp(input: String, position: Int) {
        val cmd = "pm uninstall -k --user 0 $input"
        viewModel.adb.sendToShellProcess(cmd)

        list.removeAt(position)
        installedApps(list)

        Toast.makeText(this, "App $input has been deleted", Toast.LENGTH_LONG).show()
    }

    private fun installedApps(list: List<PackageInfo>) {
        var appList = arrayOf<AppRow>()
        for (i in list.indices) {
            val packageInfo = list[i]
//            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val title = packageInfo.applicationInfo.packageName.toString()
            val description = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val imageId = packageInfo.applicationInfo.loadIcon(packageManager)

            appList += AppRow(title, description, imageId)
//            }
        }

        myListAdapter = AppListAdapter(this, appList, ::deleteApp)
        listView.adapter = myListAdapter
    }
}