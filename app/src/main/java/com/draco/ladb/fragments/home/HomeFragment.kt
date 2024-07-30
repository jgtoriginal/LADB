package com.draco.ladb.fragments.home

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.draco.ladb.R
import com.draco.ladb.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var appsAdapter: AppsAdapter

    // Variables for managing the visibility of the "full speed" text
    private var showRunnable: Runnable? = null
    private var hideRunnable: Runnable? = null
    private val handler = Handler()
    private var isReady: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the UI components
        setupUI()

        // Initialize ADB server
        viewModel.adb.initServer()

        // Observe LiveData and update the UI
        setupDataListeners()

        return root
    }

    private fun setupUI() {
        val swipeRefreshLayout = binding.swipeRefreshLayout
        val recyclerView = binding.recyclerView
        val searchView = binding.searchView

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_NO) {
            val greenColor = ContextCompat.getColor(requireContext(), R.color.green)
            searchView.setBackgroundColor(greenColor)
        } else {
            searchView.setBackgroundColor(Color.TRANSPARENT)
        }

        val searchTextView = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchTextView.setTextColor(Color.WHITE)
        searchTextView.setHintTextColor(Color.WHITE)

        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        recyclerView.layoutManager = LinearLayoutManager(context)

        val isAdbStarted = viewModel.adb.started.value ?: false
        appsAdapter = AppsAdapter(
            requireContext(),
            emptyList(),
            requireActivity().packageManager,
            ::deleteApp,
            isAdbStarted,
            {
                viewModel.refreshApps()
            },
        )

        recyclerView.adapter = appsAdapter

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshApps()
            swipeRefreshLayout.isRefreshing = false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                appsAdapter.filter(newText)
                return true
            }
        })
    }

    private fun setupDataListeners() {
        viewModel.adb.started.observe(viewLifecycleOwner) { ready ->
            Log.d("HomeFragment", "Connection readiness changed: $ready")
            setReadyForInput(ready)
            appsAdapter.isAdbStarted = ready
            appsAdapter.notifyDataSetChanged()
        }

        viewModel.apps.observe(viewLifecycleOwner, Observer { appsList ->
            appsAdapter.updateList(appsList)
        })
    }

    private fun setReadyForInput(ready: Boolean) {
        isReady = ready

        if (ready) {
            // Cancel any previously scheduled hide action
            hideRunnable?.let { handler.removeCallbacks(it) }

            // Schedule to show the "full speed" text after 1500ms if still ready
            showRunnable = Runnable {
                if (isReady) {
                    binding.fullSpeedText.visibility = View.VISIBLE
                }
            }.also { handler.postDelayed(it, 1500) }
        } else {
            // Cancel any previously scheduled show action
            showRunnable?.let { handler.removeCallbacks(it) }

            // Schedule to hide the "full speed" text after 1500ms
            hideRunnable = Runnable {
                binding.fullSpeedText.visibility = View.GONE
            }.also { handler.postDelayed(it, 1500) }
        }
    }

    private fun deleteApp(packageName: String) {
        val cmd = "pm uninstall -k --user 0 $packageName"
        Log.d("AppsAdapter", "Attempting to disable app: $packageName")
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.adb.sendToShellProcess(cmd)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
