package com.intersindical.cork

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
//import com.firebase.ui.auth.AuthUI
//import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
//import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
//import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.intersindical.cork.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.*

data class User (
    var email : String,
    var password : String,
    var name : String
)

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

class MainActivity : AppCompatActivity() {

    private var centres : List<Centre> = emptyList()
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionId = 42

    private lateinit var centre : Centre

    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var users : List<User> = listOf(
        User("gonzalo.alcaraz@intersindical-csc.cat","46693925", "GONZALO ALCARAZ RUIZ"),
        User("anna.alegre@intersindical-csc.cat","39905310", "ANNA ALEGRE FERNANDEZ"),
        User("carles.amigo@intersindical-csc.cat","46667185", "CARLES AMIGÓ RODRIGO"),
        User("rosa.brucart@intersindical-csc.cat","77728278", "ROSA BRUCART PRAT"),
        User("miquelangel.camacho@intersindical-csc.cat","52460266", "MIQUEL ÀNGEL CAMACHO VEGA"),
        User("montse.carreras@intersindical-csc.cat","77909824", "MONTSE CARRERAS BARNÉS"),
        User("toni.casserras@intersindical-csc.cat","39332661", "TONI CASSERRAS GASOL"),
        User("roger.castellanos@intersindical-csc.cat","43559189", "ROGER CASTELLANOS CORBERA"),
        User("gustau.castells@intersindical-csc.cat","44990832", "GUSTAU CASTELLS APARICIO"),
        User("jordi.clopes@intersindical-csc.cat","77612612", "JORDI CLOPES GARRELL"),
        User("jordi.espot@intersindical-csc.cat","46139555", "JORDI ESPOT BENAZET"),
        User("gerard.furest@intersindical-csc.cat","40452328", "GERARD FUREST DALMAU"),
        User("lluis.hurtado@intersindical-csc.cat","52595463", "LLUÍS HURTADO NOGUER"),
        User("merce.ibern@intersindical-csc.cat","43735629", "MERCÈ IBERN NOVELL"),
        User("josep.marce@intersindical-csc.cat","46631108", "JOSEP ANTON MARCÉ BUSQUETS"),
        User("ester.marles@intersindical-csc.cat","46588790", "ESTER MARLES XAUS"),
        User("elisabeth.martinez@intersindical-csc.cat","47933418", "ELI MARTÍNEZ SALAMÓ"),
        User("marta.molina@intersindical-csc.cat","38144090", "MARTA MOLINA ÀLVAREZ"),
        User("nuria.pastor@intersindical-csc.cat","40312977", "NÚRIA PASTOR GAMERO"),
        User("bernat.pelach@intersindical-csc.cat","40346988", "BERNAT PELACH SAGET"),
        User("laura.perez@intersindical-csc.cat","53070896", "LAURA PÉREZ PÉREZ"),
        User("alba.perez@intersindical-csc.cat","43698734", "ALBA PEREZ XAUS"),
        User("eloi.planas@intersindical-csc.cat","33956254", "Eloi Planas Vila"),
        User("toni.pons@intersindical-csc.cat","52605521", "JOAN ANTONI PONS ALBALAT"),
        User("judith.ribera@intersindical-csc.cat","47675111", "Judith Ribera Salvia"),
        User("angel.sagarra@intersindical-csc.cat","38111152", "ANGEL MARIA SAGARRA GUITART"),
        User("marc.santasusana@intersindical-csc.cat","47714590", "MARC SANTASUSANA CORZAN"),
        User("luard.silvestre@intersindical-csc.cat","38866187", "LUARD SILVESTRE CASTELLÓ"),
        User("aina.sola@intersindical-csc.cat","46629777", "Aina Solà Rodrigo")
    )

    private fun createUsers() {
        for (item in users) {
            auth.createUserWithEmailAndPassword(item.email, item.password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        //Log.d(TAG, "createUserWithEmail:success")
                        // val user = auth.currentUser
                        //updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        //Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        //updateUI(null)
                    }
                }
        }
    }

    // Authentication

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload();
        }
    }
    private fun reload() {
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    //Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    //Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "L'autenticació ha fallat.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
        // [END sign_in_with_email]
    }

    private fun updateUI(user : FirebaseUser?) {
        if (user != null) {
            hideKeyboard()

            binding.loginButton.visibility = View.INVISIBLE
            binding.identificat.visibility = View.INVISIBLE
            binding.email.visibility = View.INVISIBLE
            binding.password.visibility = View.INVISIBLE

            binding.school.visibility = View.VISIBLE
            binding.visitButton.visibility = View.VISIBLE
            binding.searchSchoolButton.visibility = View.VISIBLE

            // loadCentresButton.visibility = View.VISIBLE
            getLastLocation()

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        //setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Firebase Auth
        auth = Firebase.auth

        binding.school.visibility = View.INVISIBLE
        binding.visitButton.visibility = View.INVISIBLE
        binding.loadCentresButton.visibility = View.INVISIBLE
        binding.searchSchoolButton.visibility = View.INVISIBLE




        val user = Firebase.auth.currentUser
        if (user != null) {
            // User is signed in
            // updateUI(user)
            Firebase.auth.signOut()
        } else {
            // No user is signed in
        }

        loadCentres()

        // Force the creation of all users in database
        // createUsers()

        if (!isOnline()) {
            Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                .show()
        }

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
                val myEmail : String = binding.email.text.toString()
                val myPassword : String = binding.password.text.toString()

                signIn(myEmail, myPassword)

            }
        }

        val url = "https://cork-86201-default-rtdb.europe-west1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(url)

        binding.visitButton.setOnClickListener {
            val currentTime = java.util.Calendar.getInstance()
            centre.visitTime = currentTime.time.toString()
            centre.user = auth.currentUser!!.email.toString()
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
        for (item in users) {
            if (item.password == loginid) {
                Toast.makeText(
                    this@MainActivity,
                    item.name,
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
    var user : String)


/*
Val myCalendar = Calendar.getInstance()
val year = myCalendar.get(Calendar.YEAR)
val month = myCalendar.get(Calendar.MONTH)
val day = myCalendar.get(Calendar.DAY_OF_MONTH)

 */