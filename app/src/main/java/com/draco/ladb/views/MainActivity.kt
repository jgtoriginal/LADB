package com.draco.ladb.views

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.Formatter.formatShortFileSize
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.draco.ladb.R
import com.draco.ladb.viewmodels.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class AppRow(
    val title:String,
    val description:String,
    val imageId:Drawable,
    val packageId:String,
)
class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var list: MutableList<PackageInfo>
    private lateinit var myListAdapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        viewModel.adb.initializeClient()
        title = "Installed Apps"
        listView = findViewById(R.id.listView)
        list = packageManager.getInstalledPackages(0)
        installedApps(list)
    }

    private fun deleteApp(input: String, position: Int) {
        val cmd = "pm uninstall -k --user 0 $input"
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.adb.sendToShellProcess(cmd)
        }

        /* TODO [FIX]
        *  Need to remove item from list only once sendToShellProcess returns success.
        *  At the moment we are not observing the result of sendToShellProcess.
        *  We are just assuming that it will succeed, and based on that assumption we remove the item from the list.
        * */
        list.removeAt(position)
        installedApps(list)

        Toast.makeText(this, "App $input has been deleted", Toast.LENGTH_LONG).show()
    }

    private fun installedApps(list: List<PackageInfo>) {
        var appList = arrayOf<AppRow>()

        for (i in list.indices) {
            val packageInfo = list[i]
            if (packageInfo!!.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
//            if (packageInfo!!.applicationInfo.category !== 0) {
                val title = packageInfo.applicationInfo.loadLabel(packageManager).toString()


                val installedDate = SimpleDateFormat("dd/MM/yy").format(Date(packageInfo!!.firstInstallTime)).toString()
                val file = File(packageInfo!!.applicationInfo.publicSourceDir)
                val fileSize = formatShortFileSize(this, file.length())

                val description = "$installedDate, $fileSize"
                val imageId = packageInfo.applicationInfo.loadIcon(packageManager)
                val packageId = packageInfo.applicationInfo.packageName.toString()

                appList += AppRow(title, description, imageId, packageId)
            }
        }

        myListAdapter = AppListAdapter(this, appList, ::deleteApp)
        listView.adapter = myListAdapter
    }
}