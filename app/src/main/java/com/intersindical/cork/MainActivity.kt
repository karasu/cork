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
import kotlin.math.*

data class Nis (
    var ni : String,
    var nom : String)

class MainActivity : AppCompatActivity() {
    private var myDownLoadId: Long = -1
    private var centres : List<Centre> = emptyList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_ID = 42
    private val EPSILON = 100
    private lateinit var centre : Centre

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
        searchSchoolButton.visibility = View.INVISIBLE

        //identificat.visibility = View.INVISIBLE
        //nif.visibility = View.INVISIBLE
        //loginButton.visibility = View.INVISIBLE

        loadCentres()

        if (!isOnline()) {
            Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                .show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadCentresButton.setOnClickListener {
            loadCentres()
        }

        searchSchoolButton.setOnClickListener {
            getLastLocation()
        }

        loginButton.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG)
                    .show()
            }
            else {
                if (checkLogin(nif.text.toString())) {
                    // Login ok
                    loginButton.visibility = View.INVISIBLE
                    identificat.visibility = View.INVISIBLE
                    nif.visibility = View.INVISIBLE

                    school.visibility = View.VISIBLE
                    visitButton.visibility = View.VISIBLE
                    searchSchoolButton.visibility = View.VISIBLE

                    // loadCentresButton.visibility = View.VISIBLE
                    getLastLocation()

                } else {
                    Toast.makeText(this@MainActivity, R.string.invalidlogin, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        //visitButton.setOnClickListener {

        //}
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


    /*
    dlon = lon2 - lon1
dlat = lat2 - lat1
a = sin^2(dlat/2) + cos(lat1) * cos(lat2) * sin^2(dlon/2)
c = 2 * arcsin(min(1,sqrt(a)))
d = R * c


const R = 6371e3; // metres
const φ1 = lat1 * Math.PI/180; // φ, λ in radians
const φ2 = lat2 * Math.PI/180;
const Δφ = (lat2-lat1) * Math.PI/180;
const Δλ = (lon2-lon1) * Math.PI/180;

const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
          Math.cos(φ1) * Math.cos(φ2) *
          Math.sin(Δλ/2) * Math.sin(Δλ/2);
const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

const d = R * c; // in metres
     */

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Double {
        val radius = 6371e3; // metres
        val rlat1 = lat1 * PI / 180 // φ1
        val rlat2 = lat2 * PI / 180 // φ2
        val dlat = (lat2 - lat1) * PI / 180 // Δφ
        val dlon = (lon2 - lon1) * PI / 180 // Δλ

        val a = sin(dlat/2).pow(2) + cos(rlat1) * cos(rlat2) * sin(dlon/2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))

        val dist = radius * c

        return dist
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

    fun iguals(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Boolean {
        // Ens diu la distància en m
        return distance(lat1, lon1, lat2, lon2) < EPSILON
    }

    fun updateCentre(latitude : Double, longitude : Double) : Boolean {
        for (item in centres) {
            if (item.Coordenades_GEO_X != null &&
                    item.Coordenades_GEO_Y != null) {
                if (iguals(latitude, item.Coordenades_GEO_Y!!, longitude, item.Coordenades_GEO_X!!)) {
                    centre = item
                    school.text = item.Nom
                    Toast.makeText(
                        this@MainActivity,
                        item.Nom,
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
            }
        }
        Toast.makeText(
            this@MainActivity,
            "No s'ha trobat cap centre!",
            Toast.LENGTH_SHORT).show()
        return false
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
    var email : String?=null)


/*
Val myCalendar = Calendar.getInstance()
val year = myCalendar.get(Calendar.YEAR)
val month = myCalendar.get(Calendar.MONTH)
val day = myCalendar.get(Calendar.DAY_OF_MONTH)

 */