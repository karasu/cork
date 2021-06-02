package com.intersindical.cork

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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
//import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

data class Nis (
    var ni : String,
    var nom : String)

class MainActivity : AppCompatActivity() {
    private var myDownLoadId: Long = -1
    private var centres : List<Centre> = emptyList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val LOCATION_PERMISSION_ID = 42

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        school.visibility = View.INVISIBLE
        visitButton.visibility = View.INVISIBLE
        loadCentresButton.visibility = View.INVISIBLE

        loadCentres()

        if (!isOnline()) {
            Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                .show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation()

        loadCentresButton.setOnClickListener {
            loadCentres()
        }

        searchSchoolButton.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                    .show()
            }
            else {
                getLastLocation()
            }
        }

        loginButton.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                    .show()
            }
            else {

                if (checkLogin(nif.text.toString())) {
                    // Login ok
                    Toast.makeText(this@MainActivity, R.string.welcome, Toast.LENGTH_SHORT)
                        .show()
                    loginButton.visibility = View.INVISIBLE
                    identificat.visibility = View.INVISIBLE
                    nif.visibility = View.INVISIBLE

                    school.visibility = View.VISIBLE
                    visitButton.visibility = View.VISIBLE
                    // loadCentresButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@MainActivity, R.string.invalidlogin, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        //findViewById<TextView>(R.id.latTextView).text = location.latitude.toString()
                        //findViewById<TextView>(R.id.lonTextView).text = location.longitude.toString()
                        Toast.makeText(
                            this@MainActivity,
                            location.latitude.toString() + ", " + location.longitude.toString(),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var lastLocation: Location = locationResult.lastLocation

            //findViewById<TextView>(R.id.latTextView).text = mLastLocation.latitude.toString()
            //findViewById<TextView>(R.id.lonTextView).text = mLastLocation.longitude.toString()
            Toast.makeText(
                this@MainActivity,
                lastLocation.latitude.toString() + ", " + lastLocation.longitude.toString(),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
            LOCATION_PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
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
}


data class Centre(
    var Codi : String?=null,
    var Nom : String?=null,
    var Nom_comarca : String?=null,
    var Nom_municipi : String?=null,
    var Coordenades_GEO_X : Float?=null,
    var Coordenades_GEO_Y : Float?=null,
    @SerializedName("E-mail_centre")
    var email : String?=null)


/*
Val myCalendar = Calendar.getInstance()
val year = myCalendar.get(Calendar.YEAR)
val month = myCalendar.get(Calendar.MONTH)
val day = myCalendar.get(Calendar.DAY_OF_MONTH)

 */