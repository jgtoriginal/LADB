package com.draco.ladb.fragments.home

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.draco.ladb.databinding.FragmentHomeBinding
import com.draco.ladb.viewmodels.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.draco.ladb.R

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var appsAdapter: AppsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Find the SwipeRefreshLayout, RecyclerView, and SearchView
        val swipeRefreshLayout = binding.swipeRefreshLayout
        val recyclerView = binding.recyclerView
        val searchView = binding.searchView

        // Set background color of the entire SearchView
        val greenColor = ContextCompat.getColor(requireContext(), R.color.green)
        searchView.setBackgroundColor(greenColor)

        val searchTextView = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchTextView.setTextColor(Color.WHITE) // Set the query text color to white
        searchTextView.setHintTextColor(Color.WHITE)

        // Access the search icon (magnifier)
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN) // Set the icon color to white

        // Access the clear button (X icon)
        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)


        recyclerView.layoutManager = LinearLayoutManager(context)

        appsAdapter = AppsAdapter(
            requireContext(),
            emptyList(),
            requireActivity().packageManager,
            ::deleteApp
        ) {
            // Callback to refresh the list when an app is uninstalled or disabled
            viewModel.refreshApps()
        }

        recyclerView.adapter = appsAdapter

        // Observe the apps LiveData and update the adapter
        viewModel.apps.observe(viewLifecycleOwner, Observer { appsList ->
            appsAdapter.updateList(appsList)
        })

        // Initialize ADB server
        viewModel.adb.initServer()

        // Set up SwipeRefreshLayout listener
        swipeRefreshLayout.setOnRefreshListener {
            // Trigger refresh
            viewModel.refreshApps()
            swipeRefreshLayout.isRefreshing = false // Stop the refresh animation
        }

        // Set up SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                appsAdapter.filter(newText)
                return true
            }
        })

        return root
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
