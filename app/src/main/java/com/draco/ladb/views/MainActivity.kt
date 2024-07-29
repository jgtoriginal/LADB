package com.draco.ladb.views

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.draco.ladb.BuildConfig
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityMainBinding
import com.draco.ladb.fragments.home.HomeFragment
import com.draco.ladb.viewmodels.DashboardViewModel
import com.draco.ladb.viewmodels.MainActivityViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupKeyboardVisibilityListener(navView)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_home) {
                supportActionBar?.hide()
            } else {
                supportActionBar?.show()
            }
        }

        if (viewModel.isPairing.value != true)
            viewModel.startADBServer()
    }

    private fun setupKeyboardVisibilityListener(navView: BottomNavigationView) {

        val rootView = findViewById<View>(android.R.id.content)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom

            /** WIP: hiding tabs for now.
             * not showing all tabs to end users for now
             * */

            if (BuildConfig.DEBUG) {
                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is open
                    navView.visibility = View.GONE
                    updateRecyclerViewMargin(recyclerView, 0)
                } else {
                    // Keyboard is closed
                    navView.visibility = View.VISIBLE
                    val actionBarSize = getActionBarSize()
                    updateRecyclerViewMargin(recyclerView, actionBarSize)
                }
            } else {
                navView.visibility = View.GONE
                updateRecyclerViewMargin(recyclerView, 0)
            }
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun updateRecyclerViewMargin(recyclerView: RecyclerView, bottomMargin: Int) {
        val layoutParams = recyclerView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = bottomMargin
        recyclerView.layoutParams = layoutParams
    }

    private fun getActionBarSize(): Int {
        val typedValue = TypedValue()
        val attribute = intArrayOf(android.R.attr.actionBarSize)
        val typedArray = obtainStyledAttributes(typedValue.data, attribute)
        val actionBarSize = typedArray.getDimensionPixelSize(0, -1)
        typedArray.recycle()
        return actionBarSize
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the global layout listener to avoid memory leaks
        val rootView = findViewById<View>(android.R.id.content)
        if (::globalLayoutListener.isInitialized) {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        }
    }
}
