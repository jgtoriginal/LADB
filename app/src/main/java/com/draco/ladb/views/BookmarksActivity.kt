package com.draco.ladb.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import com.draco.ladb.R
import com.draco.ladb.viewmodels.MainActivityViewModel


class BookmarksActivity: AppCompatActivity() {
    lateinit var listView: ListView
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        title = "Installed Apps"
        listView = findViewById(R.id.listView)
        installedApps()

        listView.setOnItemClickListener(){adapterView, view, position, id ->
            val itemAtPos = adapterView.getItemAtPosition(position)
            val itemIdAtPos = adapterView.getItemIdAtPosition(position)
            val text = "pm uninstall -k --user 0 $itemAtPos"
            viewModel.adb.sendToShellProcess(text)
            this.recreate()
            Toast.makeText(this, "Click on item at $itemAtPos its item id $itemIdAtPos", Toast.LENGTH_LONG).show()
        }
    }
    private fun installedApps() {
        val list = packageManager.getInstalledPackages(0)
        var appList = arrayOf<String?>()
        var title = arrayOf<String>()
        var description = arrayOf<String>()
        var imageId = arrayOf<Drawable>()
        for (i in list.indices) {
            val packageInfo = list[i]
//            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                val appIcon = packageInfo.applicationInfo.loadIcon(packageManager)
                val packageName = packageInfo.applicationInfo.packageName.toString()
                Log.e("App List$i", appIcon.toString())
                appList += "$appName-$packageName"
                title += appName
                description += packageName
                imageId += appIcon
//            }
        }

//        val myListAdapter = AppListAdapter(this, description, title, imageId)
//        listView.adapter = myListAdapter
    }
}