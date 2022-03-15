package com.draco.ladb.views

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.draco.ladb.R
import com.draco.ladb.viewmodels.MainActivityViewModel


class MainActivity : AppCompatActivity() {
    lateinit var listView: ListView
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        title = "Installed Apps"
        listView = findViewById(R.id.listView)
        installedApps()
    }

    fun deleteApp(input: String) {
        Log.e("TAG","test another message: $input")
        val cmd = "pm uninstall -k --user 0 $input"
        viewModel.adb.sendToShellProcess(cmd)
        this.recreate()
    }

    private fun installedApps() {
        val list = packageManager.getInstalledPackages(0)
        var title = arrayOf<String>()
        var description = arrayOf<String>()
        var imageId = arrayOf<Drawable>()
        for (i in list.indices) {
            val packageInfo = list[i]
//            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            title += packageInfo.applicationInfo.packageName.toString()
            description += packageInfo.applicationInfo.loadLabel(packageManager).toString()
            imageId += packageInfo.applicationInfo.loadIcon(packageManager)
//            }
        }

        val myListAdapter = AppListAdapter(this, title, description, imageId, ::deleteApp)
        listView.adapter = myListAdapter
    }
}