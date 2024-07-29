package com.draco.ladb.fragments.dashboard

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.draco.ladb.BuildConfig
import com.draco.ladb.R
import com.draco.ladb.databinding.FragmentDashboardBinding
import com.draco.ladb.viewmodels.DashboardViewModel
import com.draco.ladb.viewmodels.MainActivityViewModel
import com.draco.ladb.views.HelpActivity
import com.draco.ladb.views.BookmarksActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    // ViewModel for the activity
    private val viewModel: DashboardViewModel by viewModels()
    // View binding for the fragment
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    // Dialog for pairing
    private lateinit var pairDialog: MaterialAlertDialogBuilder

    // Last command entered by the user
    private var lastCommand = ""

    // Register a result callback for the bookmark activity
    private val bookmarkGetResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val text = it.data?.getStringExtra(Intent.EXTRA_TEXT) ?: return@registerForActivityResult
        binding.command.setText(text)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout and initialize view binding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        setHasOptionsMenu(true) // Indicate that this fragment has an options menu

        setupUI() // Set up the UI elements
        setupDataListeners() // Set up data listeners for LiveData objects

        // If not currently pairing, start the pairing process
        if (viewModel.isPairing.value != true)
            pairAndStart()

        return root
    }

    // Set up the UI components and event listeners
    private fun setupUI() {
        pairDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pair_title)
            .setCancelable(false)
            .setView(R.layout.dialog_pair)
            .setPositiveButton(R.string.pair, null)
            .setNegativeButton(R.string.help, null)
            .setNeutralButton(R.string.skip, null)

        // Handle enter key events for the command input
        binding.command.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {
                sendCommandToADB() // Send command when enter is pressed
                return@setOnKeyListener true
            } else {
                return@setOnKeyListener false
            }
        }

        // Handle editor action events (such as IME_ACTION_SEND) for the command input
        binding.command.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommandToADB() // Send command when IME_ACTION_SEND is triggered
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
        }
    }

    // Send the command entered in the input field to ADB
    private fun sendCommandToADB() {
        val text = binding.command.text.toString()
        lastCommand = text
        binding.command.text = null // Clear the input field
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.adb.sendToShellProcess(text) // Send the command in a background thread
        }
    }

    // Set the UI state based on whether the input is ready
    private fun setReadyForInput(ready: Boolean) {
        binding.command.isEnabled = ready // Enable/disable the command input field
        binding.commandContainer.hint =
            if (ready) getString(R.string.command_hint) else getString(R.string.command_hint_waiting)
        binding.progress.visibility = if (ready) View.INVISIBLE else View.VISIBLE // Show/hide progress bar
    }

    // Set up listeners for LiveData objects in the ViewModel
    private fun setupDataListeners() {
        /* Update the output text */
        viewModel.outputText.observe(viewLifecycleOwner) { newText ->
            binding.output.text = newText // Set the output text
            binding.outputScrollview.post {
                binding.outputScrollview.fullScroll(ScrollView.FOCUS_DOWN) // Scroll to the bottom
                binding.command.requestFocus()
                val imm = getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.showSoftInput(binding.command, InputMethodManager.SHOW_IMPLICIT) // Show the keyboard
            }
        }

        /* Restart the activity on reset */
        viewModel.adb.closed.observe(viewLifecycleOwner) { closed ->
            if (closed == true) {
                activity?.recreate() // Restart the activity if ADB connection is closed
            }
        }

        /* Prepare progress bar, pairing latch, and script executing */
        viewModel.adb.started.observe(viewLifecycleOwner) { started ->
            setReadyForInput(started == true) // Update UI based on whether ADB is started
        }
    }

    // Start the pairing process if necessary and start the ADB server
    private fun pairAndStart() {
        if (viewModel.needsToPair()) {
            viewModel.adb.debug("Requesting pairing information")
            askToPair { thisPairSuccess ->
                if (thisPairSuccess) {
                    viewModel.setPairedBefore(true)
                    viewModel.startADBServer()
                } else {
                    /* Failed; try again! */
                    viewModel.adb.debug("Failed to pair! Trying again...")
                    requireActivity().runOnUiThread { pairAndStart() } // Retry pairing on the UI thread
                }
            }
        } else {
            viewModel.startADBServer()
        }
    }

    // Show a dialog to ask the user for pairing information
    private fun askToPair(callback: ((Boolean) -> (Unit))? = null) {
        pairDialog
            .create()
            .apply {
                setOnShowListener {
                    // Handle positive button click
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val port = findViewById<TextInputEditText>(R.id.port)!!.text.toString()
                        val code = findViewById<TextInputEditText>(R.id.code)!!.text.toString()
                        dismiss()

                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.adb.debug("Trying to pair...")
                            val success = viewModel.adb.pair(port, code) // Try to pair with ADB
                            callback?.invoke(success) // Invoke callback with the result
                        }
                    }

                    // Handle negative button click
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tutorial_url)))
                        try {
                            startActivity(intent) // Open tutorial URL
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Snackbar.make(
                                binding.output,
                                getString(R.string.snackbar_intent_failed),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Handle neutral button click
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit(true) {
                            putBoolean(getString(R.string.auto_shell_key), false)
                        }
                        dismiss()
                        callback?.invoke(true)
                    }
                }
            }
            .show()
    }

    // Inflate the options menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Handle options menu item selection
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bookmarks -> {
                val intent = Intent(requireContext(), BookmarksActivity::class.java)
                    .putExtra(Intent.EXTRA_TEXT, binding.command.text.toString())
                bookmarkGetResult.launch(intent) // Launch bookmarks activity
                true
            }

            R.id.last_command -> {
                binding.command.setText(lastCommand) // Set the last command in the input field
                binding.command.setSelection(lastCommand.length)
                true
            }

            R.id.more -> {
                val intent = Intent(requireContext(), HelpActivity::class.java)
                startActivity(intent) // Launch help activity
                true
            }

            R.id.share -> {
                try {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        viewModel.adb.outputBufferFile
                    )
                    val intent = Intent(Intent.ACTION_SEND)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setType("file/*")
                    startActivity(intent) // Share the output file
                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(binding.output, getString(R.string.snackbar_intent_failed), Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.dismiss)) {}
                        .show()
                }
                true
            }

            R.id.clear -> {
                viewModel.clearOutputText() // Clear the output text
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Clean up resources when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
