package com.draco.ladb.fragments.home

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.draco.ladb.fragments.home.AppInfo
import com.draco.ladb.utils.ADB
import com.draco.ladb.viewmodels.MainActivityViewModel

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableLiveData<List<AppInfo>>().apply {
        value = getInstalledApps()
    }
    val apps: LiveData<List<AppInfo>> = _apps

    val adb = ADB.getInstance(application.applicationContext)

    fun refreshApps() {
        _apps.value = getInstalledApps()
    }

    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = getApplication<Application>().packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val launchableApps = packages.filter { app ->
            packageManager.getLaunchIntentForPackage(app.packageName) != null
        }
        val appList = launchableApps.map {
            val appName = packageManager.getApplicationLabel(it).toString()
            val appIcon = packageManager.getApplicationIcon(it.packageName)
            val isSystemApp = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(appName, it.packageName, appIcon, isSystemApp)
        }
        // Sort apps by appName alphabetically
        return appList.sortedBy { it.appName }
    }
}
