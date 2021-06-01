package com.intersindical.cork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var myDownLoadId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        school.visibility = View.INVISIBLE
        visitButton.visibility = View.INVISIBLE

        Toast.makeText(this@MainActivity, "Carregant centres...", Toast.LENGTH_LONG).show()
        runBlocking {
            launch {
                val jsontext = resources.openRawResource(R.raw.centres_educatius)
                    .bufferedReader().use { it.readText() }
                val centres = Gson().fromJson(jsontext, Centre::class.java)
            }
        }



        if (!isOnline()) {
            Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG).show()
        }

        loginButton.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this@MainActivity, "Sense connexió!", Toast.LENGTH_LONG).show()
            }
            else {

                if (checkLogin(nif.text.toString())) {
                    // Login ok
                    Toast.makeText(this@MainActivity, R.string.welcome, Toast.LENGTH_SHORT).show()
                    loginButton.visibility = View.INVISIBLE
                    identificat.visibility = View.INVISIBLE
                    nif.visibility = View.INVISIBLE
                    school.visibility = View.VISIBLE
                    visitButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@MainActivity, R.string.invalidlogin, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun isOnline(): Boolean {
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