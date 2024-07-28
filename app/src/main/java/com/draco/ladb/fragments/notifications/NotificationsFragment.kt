package com.draco.ladb.fragments.notifications

import android.content.ContentResolver
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.draco.ladb.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val buttonSetDns: Button = binding.buttonSetDns
        buttonSetDns.setOnClickListener {
            setPrivateDns("dns.example.com")  // Replace with your DNS host
        }

        return root
    }

    private fun setPrivateDns(dnsHost: String) {
        try {
            val resolver: ContentResolver = requireContext().contentResolver
            Settings.Secure.putString(resolver, "private_dns_mode", "hostname")
            Settings.Secure.putString(resolver, "private_dns_specifier", dnsHost)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
