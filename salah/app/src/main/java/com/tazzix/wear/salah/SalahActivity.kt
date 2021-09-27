// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.tazzix.wear.salah

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.tazzix.wear.salah.complication.SalahComplicationProviderService.Companion.forceComplicationUpdate
import com.tazzix.wear.salah.data.LocationViewModel
import com.tazzix.wear.salah.data.MyLocation
import com.tazzix.wear.salah.data.ResolvedLocation
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitSingle

class SalahActivity : FragmentActivity() {
    private lateinit var locationViewModel: LocationViewModel

    private lateinit var loc: TextView
    private lateinit var fajr: TextView
    private lateinit var sunrise: TextView
    private lateinit var dhuhur: TextView
    private lateinit var asr: TextView
    private lateinit var maghrib: TextView
    private lateinit var isha: TextView
    private lateinit var prefs: ImageButton

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prayer_times)
        loc = findViewById(R.id.location)
        fajr = findViewById(R.id.fajr)
        sunrise = findViewById(R.id.sunrise)
        dhuhur = findViewById(R.id.dhuhur)
        asr = findViewById(R.id.asr)
        maghrib = findViewById(R.id.maghrib)
        isha = findViewById(R.id.isha)

        prefs = findViewById(R.id.buttom_prefs)
        prefs.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        })

        locationViewModel = LocationViewModel(applicationContext)

        lifecycleScope.launch {
            whenStarted {
                refreshView()
            }
            checkPermissions()

            val location = locationViewModel.readLocationResult()

            if (location is ResolvedLocation) {
                val shPrefs = getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE)
                val method = shPrefs.getInt("METHOD", 0)
                val asrCalc = shPrefs.getInt("ASR_CALC", 0)
                val location2 = MyLocation(location.location.latitude, location.location.longitude, method, asrCalc)
                renderUI(location2, location.location.time, true, "")
                // DONE: save location in SharedPrefs
                val editor = shPrefs.edit()
                editor.putString("LT", location2.latitude.toString())
                editor.putString("LL", location2.longitude.toString())
                editor.putLong("TM", location.location.time)
                editor.apply()
            } else {
                loc.setText(R.string.location_error)
            }
        }

        forceComplicationUpdate()
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    fun refreshView() {
        // DONE: read location from shared prefs
        val sharedPreference =  getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE)
        val lat = sharedPreference.getString("LT", "0.00")?.toDouble()!!
        val lon = sharedPreference.getString("LL", "0.00")?.toDouble()!!
        val method = sharedPreference.getInt("METHOD", 0)
        val asrCalc = sharedPreference.getInt("ASR_CALC", 0)
        val tm = sharedPreference.getLong("TM", 0)
        val locVal = sharedPreference.getString("LC", "N/A")!!

        // DONE: if location not found, put tap target
        if (lat!=0.0 && lon!=0.0) {
            val location2 = MyLocation(lat, lon, method, asrCalc)
            renderUI(location2,tm, false, locVal)
        }
    }

    fun renderUI (mylocation: MyLocation, time: Long, refLoc: Boolean, locVal: String) {
        val pInfo = getAddressDescription(mylocation, refLoc)
        var locality = pInfo.locality
        if (!refLoc) {
            locality = locVal
        } else {
            val editor = getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE).edit()
            editor.putString("LC", locality)
            editor.apply()
        }
        loc.text = getString(
            R.string.address_as_of_time_activity,
            locality,
            getTimeAgo(time)
        )
        fajr.text = pInfo.fajr.toString().dropLast(3)
        sunrise.text = pInfo.sunrise.toString().dropLast(3)
        dhuhur.text = pInfo.dhuhur.toString().dropLast(3)
        asr.text = pInfo.asr.toString().dropLast(3)
        maghrib.text = pInfo.maghrib.toString().dropLast(3)
        isha.text = pInfo.isha.toString().dropLast(3)
    }

    override fun onStop() {
        forceComplicationUpdate()
        super.onStop()
    }

    suspend fun checkPermissions() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .doOnNext { isGranted ->
                if (!isGranted) throw SecurityException("No location permission")
            }.awaitSingle()
    }

    companion object {
        fun Context.tapAction(): PendingIntent? {
            val intent = Intent(this, SalahActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}