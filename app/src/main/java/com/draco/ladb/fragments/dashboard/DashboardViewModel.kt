package com.draco.ladb.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.draco.ladb.BuildConfig
import com.draco.ladb.R
import com.draco.ladb.utils.ADB
import com.github.javiersantos.piracychecker.PiracyChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _outputText = MutableLiveData<String>()
    val outputText: LiveData<String> = _outputText

    val isPairing = MutableLiveData<Boolean>()

    private var checker: PiracyChecker? = null
    private val sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(application.applicationContext)

    val adb = ADB.getInstance(application.applicationContext)

    init {
        startOutputThread()
    }

    fun startADBServer(callback: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = adb.initServer()
            if (success) startShellDeathThread()
            callback?.invoke(success)
        }
    }

    fun piracyCheck(context: Context) {
        if (checker != null || !BuildConfig.ANTI_PIRACY)
            return

        checker = PiracyChecker(context).apply {
            enableGooglePlayLicensing("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoRTOoEZ/IFfA/JkBFIrZqLq7N66JtJFTn/5C2QMO2EIY6hG4yZ5YTA3JrbJuuGVzQE8j29s6Lwu+19KKZcITTkZjfgl2Zku8dWQKZFt46f7mh8s1spzzc6rmSWIBPZUxN6fIIz8ar+wzyZdu3z+Iiy31dUa11Pyh82oOsWH7514AYGeIDDlvB1vSfNF/9ycEqTv5UAOgHxqZ205C1VVydJyCEwWWVJtQ+Z5zRaocI6NGaYRopyZteCEdKkBsZ69vohk4zr2SpllM5+PKb1yM7zfsiFZZanp4JWDJ3jRjEHC4s66elWG45yQi+KvWRDR25MPXhdQ9+DMfF2Ao1NTrgQIDAQAB")
            saveResultToSharedPreferences(
                sharedPreferences,
                context.getString(R.string.pref_key_verified)
            )
        }

        val verified = sharedPreferences.getBoolean(context.getString(R.string.pref_key_verified), false)
        if (!verified) checker?.start()
    }

    private fun startOutputThread() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val out = readOutputFile(adb.outputBufferFile)
                val currentText = _outputText.value
                if (out != currentText) _outputText.postValue(out)
                Thread.sleep(ADB.OUTPUT_BUFFER_DELAY_MS)
            }
        }
    }

    private fun startShellDeathThread() {
        viewModelScope.launch(Dispatchers.IO) {
            adb.waitForDeathAndReset()
        }
    }

    fun clearOutputText() {
        adb.outputBufferFile.writeText("")
    }

    fun needsToPair(): Boolean {
        return !sharedPreferences.getBoolean(getApplication<Application>().getString(R.string.paired_key), false) &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    }

    fun setPairedBefore(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(getApplication<Application>().getString(R.string.paired_key), value)
        }
    }

    private fun readOutputFile(file: File): String {
        val out = ByteArray(adb.getOutputBufferSize())

        synchronized(file) {
            if (!file.exists()) return ""

            file.inputStream().use {
                val size = it.channel.size()

                if (size <= out.size) return String(it.readBytes())

                val newPos = (it.channel.size() - out.size)
                it.channel.position(newPos)
                it.read(out)
            }
        }

        return String(out)
    }
}
