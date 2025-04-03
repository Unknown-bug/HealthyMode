package com.example.HealthyMode.UI.Home_fragment.Profile

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.HealthyMode.R
import com.example.HealthyMode.UI.Auth.MainAuthentication
import com.example.HealthyMode.UI.Home.Home_screen
import com.example.HealthyMode.UI.step.StepsTrack
import com.example.HealthyMode.UI.water.Water
import com.example.HealthyMode.Utils.Constant
import com.example.HealthyMode.Utils.UIstate
import com.example.HealthyMode.databinding.FragmentProfileFragmentBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*
import kotlin.math.floor
import kotlin.math.round
import kotlin.properties.Delegates

class profile_fragment : Fragment() {
    private var userDitails: DocumentReference = Firebase.firestore.collection("user").document(FirebaseAuth.getInstance().currentUser!!.uid)
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var binding: FragmentProfileFragmentBinding
    private lateinit var dialog: Dialog
    private lateinit var viewModel: Profile_ViewModel
    private val df = DecimalFormat("#.##")
    private var height by Delegates.notNull<Double>()
    private var isDialogShown = false

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[Profile_ViewModel::class.java]
        binding.edweight.text = Constant.loadData(requireActivity(), "weight", "curr_w", "0").toString()
        getlocation()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        setupClickListeners()
        UserDetails()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            startActivity(Intent(requireActivity(), Home_screen::class.java))
            requireActivity().finish()
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            steps.setOnClickListener {
                startActivity(Intent(requireActivity(), StepsTrack::class.java))
            }
            waterT.setOnClickListener {
                startActivity(Intent(requireActivity(), Water::class.java))
            }
            logout.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireActivity(), MainAuthentication::class.java))
                requireActivity().finish()
            }
            height.setOnClickListener {
                showHeightDialog()
            }
        }
    }

    private fun showHeightDialog() {
        try {
            dialog.setContentView(R.layout.pop_height)
            val ft: NumberPicker = dialog.findViewById(R.id.ft)
            val inch: NumberPicker = dialog.findViewById(R.id.inch)
            val add: AppCompatButton = dialog.findViewById(R.id.add)

            ft.minValue = 3
            ft.maxValue = 8
            ft.value = floor(height).toInt()
            ft.wrapSelectorWheel = true

            inch.minValue = 1
            inch.maxValue = 12
            inch.value = round((height - floor(height)) * 12).toInt()
            inch.wrapSelectorWheel = true

            add.setOnClickListener {
                val newHeight = ft.value.toDouble() + (inch.value.toDouble() / 12)
                val formattedHeight = df.format(newHeight).toDouble()
                viewModel.updateHeight(formattedHeight.toString(), requireContext())
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error showing height dialog", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.P)
    fun getlocation() {
        val locationManagerr = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManagerr.isLocationEnabled) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { currentLocation: Location? ->
                if (currentLocation != null && Constant.isInternetOn(requireActivity())) {
                    try {
                        val geocoder = Geocoder(requireActivity(), Locale.getDefault())
                        val addresses = geocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val land = address.subLocality ?: ""
                            val state = address.adminArea ?: ""
                            val country = address.countryName ?: ""
                            val locationName = address.locality ?: ""
                            val FullAddress = "$land,$locationName,$state,$country"
                            binding.locat.text = FullAddress
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error getting location details", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Please Enable Your Location", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Please Enable Your Location", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun UserDetails() {
        userDitails.addSnapshotListener { it, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading user details", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (it != null && it.exists()) {
                try {
                    binding.username.text = it.data?.get("fullname")?.toString() ?: ""
                    binding.email.text = it.data?.get("email")?.toString() ?: ""
                    binding.let.text = binding.username.text.toString().firstOrNull()?.toString() ?: ""
                    val dob = it.data?.get("dob")?.toString() ?: ""
                    if (dob.length >= 4) {
                        val birthyear = dob.substring(dob.length - 4).toIntOrNull() ?: 0
                        val currentYear = LocalDate.now().year
                        binding.age.text = (currentYear - birthyear).toString()
                    }
                    binding.Gender.text = it.data?.get("gender")?.toString() ?: ""
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error processing user data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.getHeight()
        viewModel.data.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIstate.Loading -> {
                    binding.content.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UIstate.Failure -> {
                    Toast.makeText(requireContext(), state.error.toString(), Toast.LENGTH_SHORT).show()
                }
                is UIstate.Success -> {
                    binding.content.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    binding.edhight.text = state.data.toString()
                }
            }
        }

        val curruser = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = Firebase.firestore.collection("user").document(curruser.toString())
        reference.addSnapshotListener { it, e ->
            if (e != null) {
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (it != null && it.exists() && it.data != null) {
                try {
                    var heightf = it.data?.get("height")?.toString()?.toDoubleOrNull() ?: 0.0
                    height = heightf
                    heightf *= 0.305
                    var Bmi = "0"
                    val weight = Constant.loadData(requireActivity(), "weight", "curr_w", "0").toString().toDoubleOrNull() ?: 0.0
                    
                    if (weight != 0.0 && heightf != 0.0) {
                        Bmi = Math.round((weight / (heightf * heightf))).toString()
                    }
                    
                    binding.bmi.text = Bmi
                    val bm = Bmi.toDoubleOrNull() ?: 0.0
                    
                    when {
                        bm < 18.5 -> {
                            binding.measure.text = "You are underweight"
                            binding.measure.setBackgroundColor(Color.RED)
                        }
                        bm in 25.0..29.9 -> {
                            binding.measure.text = "You are Overweight"
                            binding.measure.setBackgroundColor(Color.RED)
                        }
                        bm > 30.0 -> {
                            binding.measure.text = "You are Obese Range"
                            binding.measure.setBackgroundColor(Color.YELLOW)
                        }
                        else -> {
                            binding.measure.text = "You are Normal and Healthy"
                            binding.measure.setBackgroundColor(Color.GREEN)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error calculating BMI", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog.dismiss()
    }
}