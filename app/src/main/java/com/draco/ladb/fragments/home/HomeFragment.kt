package com.draco.ladb.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.draco.ladb.databinding.FragmentHomeBinding
import com.draco.ladb.fragments.home.AppsAdapter
import com.draco.ladb.viewmodels.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private val viewModel: MainActivityViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        val appsAdapter = AppsAdapter(
            requireContext(),
            emptyList(),
            requireActivity().packageManager,
            ::deleteApp
        ) {
            // Callback to refresh the list when an app is uninstalled or disabled
            homeViewModel.refreshApps()
        }

        recyclerView.adapter = appsAdapter

        homeViewModel.apps.observe(viewLifecycleOwner) { appsList ->
            appsAdapter.updateList(appsList)
        }

        viewModel.adb.initServer()

        return root
    }


    private fun deleteApp(input: String) {
        val cmd = "pm uninstall -k --user 0 $input"

        Log.d("AppsAdapter", "Attempting to disable app: $input")
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.adb.sendToShellProcess(cmd)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
