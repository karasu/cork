package com.intersindical.cork

//import com.google.android.gms.common.GoogleApiAvailability
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
//import com.firebase.ui.auth.AuthUI
//import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
//import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
//import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.intersindical.cork.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.*

data class Nis (
    var ni : String,
    var nom : String)

class MainActivity : AppCompatActivity() {

    private var centres : List<Centre> = emptyList()
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionId = 42

    private lateinit var centre : Centre

    // private lateinit var database: DatabaseReference
    private lateinit var database: FirebaseDatabase

    private lateinit var currentNIF: String

    private var nis : List<Nis> = listOf(
        Nis("46693925", "GONZALO ALCARAZ RUIZ"),
        Nis("39905310", "ANNA ALEGRE FERNANDEZ"),
        Nis("46667185", "CARLES AMIGÓ RODRIGO"),
        Nis("77728278", "ROSA BRUCART PRAT"),
        Nis("52460266", "MIQUEL ÀNGEL CAMACHO VEGA"),
        Nis("77909824", "MONTSE CARRERAS BARNÉS"),
        Nis("39332661", "TONI CASSERRAS GASOL"),
        Nis("43559189", "ROGER CASTELLANOS CORBERA"),
        Nis("44990832", "GUSTAU CASTELLS APARICIO"),
        Nis("77612612", "JORDI CLOPES GARRELL"),
        Nis("46139555", "JORDI ESPOT BENAZET"),
        Nis("40452328", "GERARD FUREST DALMAU"),
        Nis("52595463", "LLUÍS HURTADO NOGUER"),
        Nis("43735629", "MERCÈ IBERN NOVELL"),
        Nis("46631108", "JOSEP ANTON MARCÉ BUSQUETS"),
        Nis("46588790", "ESTER MARLES XAUS"),
        Nis("47933418", "ELI MARTÍNEZ SALAMÓ"),
        Nis("38144090", "MARTA MOLINA ÀLVAREZ"),
        Nis("40312977", "NÚRIA PASTOR GAMERO"),
        Nis("40346988", "BERNAT PELACH SAGET"),
        Nis("53070896", "LAURA PÉREZ PÉREZ"),
        Nis("43698734", "ALBA PEREZ XAUS"),
        Nis("33956254", "Eloi Planas Vila"),
        Nis("52605521", "JOAN ANTONI PONS ALBALAT"),
        Nis("47675111", "Judith Ribera Salvia"),
        Nis("38111152", "ANGEL MARIA SAGARRA GUITART"),
        Nis("47714590", "MARC SANTASUSANA CORZAN"),
        Nis("38866187", "LUARD SILVESTRE CASTELLÓ"),
        Nis("46629777", "Aina Solà Rodrigo")
    )

    /*
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    private fun createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.TwitterBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
        // [END auth_fui_create_intent]
    }
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }

     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //setContentView(R.layout.activity_main)

        binding.school.visibility = View.INVISIBLE
        binding.visitButton.visibility = View.INVISIBLE
        binding.loadCentresButton.visibility = View.INVISIBLE
        binding.searchSchoolButton.visibility = View.INVISIBLE

        //identificat.visibility = View.INVISIBLE
        //nif.visibility = View.INVISIBLE
        //loginButton.visibility = View.INVISIBLE

        loadCentres()

        if (!isOnline()) {
            Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                .show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.loadCentresButton.setOnClickListener {
            loadCentres()
        }

        binding.searchSchoolButton.setOnClickListener {
            requestNewLocationData()
        }

        binding.loginButton.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                    .show()
            }
            else {
                if (checkLogin(binding.nif.text.toString())) {
                    // Login ok
                    currentNIF = binding.nif.text.toString()
                    binding.loginButton.visibility = View.INVISIBLE
                    binding.identificat.visibility = View.INVISIBLE
                    binding.nif.visibility = View.INVISIBLE

                    binding.school.visibility = View.VISIBLE
                    binding.visitButton.visibility = View.VISIBLE
                    binding.searchSchoolButton.visibility = View.VISIBLE

                    // loadCentresButton.visibility = View.VISIBLE
                    getLastLocation()

                } else {
                    Toast.makeText(this@MainActivity, R.string.invalidlogin, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        val url = "https://cork-86201-default-rtdb.europe-west1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(url)

        binding.visitButton.setOnClickListener {
            val currentTime = java.util.Calendar.getInstance()
            centre.visitTime = currentTime.toString()
            centre.currentNIF = currentNIF
            database.getReference("centres").child(centre.Codi!!).setValue(centre)

            Toast.makeText(
                this@MainActivity,
                "Centre " + centre.Nom + " marcat com a visitat!",
                Toast.LENGTH_LONG).show()
        }

    }

    // ---------------------------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        /*
                        Toast.makeText(
                            this@MainActivity,
                            location.latitude.toString() + ", " + location.longitude.toString(),
                            Toast.LENGTH_SHORT).show()
                         */
                        updateCentre(location.latitude, location.longitude)
                    }
                }
            } else {
                Toast.makeText(this, "Activa la localització", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()!!)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation: Location = locationResult.lastLocation
            /*
            Toast.makeText(
                this@MainActivity,
                lastLocation.latitude.toString() + ", " + lastLocation.longitude.toString(),
                Toast.LENGTH_SHORT).show()
            */
            updateCentre(lastLocation.latitude, lastLocation.longitude)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            locationPermissionId
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Double {
        val radius = 6371e3 // metres
        val rlat1 = lat1 * PI / 180 // φ1
        val rlat2 = lat2 * PI / 180 // φ2
        val dlat = (lat2 - lat1) * PI / 180 // Δφ
        val dlon = (lon2 - lon1) * PI / 180 // Δλ

        val a = sin(dlat/2).pow(2) + cos(rlat1) * cos(rlat2) * sin(dlon/2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))

        return radius * c
    }

    // ---------------------------------------------------------------------------------------------

    private fun loadCentres() {
        Toast.makeText(this@MainActivity, "Carregant centres...", Toast.LENGTH_LONG).show()
        val arrayCentreType = object : TypeToken<List<Centre>>() {}.type
        runBlocking {
            launch {
                val jsontext = resources.openRawResource(R.raw.centres_educatius)
                    .bufferedReader().use { it.readText() }
                centres = Gson().fromJson(jsontext, arrayCentreType)
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    fun checkLogin(loginid: String) : Boolean {
        for (item in nis) {
            if (item.ni == loginid) {
                Toast.makeText(
                    this@MainActivity,
                    item.nom,
                    Toast.LENGTH_SHORT
                )
                    .show()
                return true
            }
        }
        return false
    }

    @SuppressLint("SetTextI18n")
    fun updateCentre(latitude : Double, longitude : Double) : Boolean {
        var mindist = -1.0
        lateinit var minitem : Centre

        for (item in centres) {
            if (item.Coordenades_GEO_X != null &&
                item.Coordenades_GEO_Y != null) {
                    val dist = distance(
                        latitude, longitude,
                        item.Coordenades_GEO_Y!!, item.Coordenades_GEO_X!!)
                if (dist < mindist || mindist < 0) {
                    mindist = dist
                    minitem = item
                }
            }
        }
        if (mindist >= 0) {
            centre = minitem
            binding.school.text = minitem.Nom + " (" + mindist.toInt() + "m)"
            Toast.makeText(
                this@MainActivity,
                minitem.Nom,
                Toast.LENGTH_SHORT
            ).show()
            return true
        } else {
            Toast.makeText(
                this@MainActivity,
                "No s'ha trobat cap centre!",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }
}


data class Centre(
    var Codi : String?=null,
    var Nom : String?=null,
    var Nom_comarca : String?=null,
    var Nom_municipi : String?=null,
    var Coordenades_GEO_X : Double?=null,
    var Coordenades_GEO_Y : Double?=null,
    @SerializedName("E-mail_centre")
    var email : String?=null,
    var visitTime : String,
    var currentNIF : String)


/*
Val myCalendar = Calendar.getInstance()
val year = myCalendar.get(Calendar.YEAR)
val month = myCalendar.get(Calendar.MONTH)
val day = myCalendar.get(Calendar.DAY_OF_MONTH)

 */