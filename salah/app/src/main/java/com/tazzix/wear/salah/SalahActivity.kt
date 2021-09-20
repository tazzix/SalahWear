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
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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

        locationViewModel = LocationViewModel(applicationContext)

        lifecycleScope.launch {
            checkPermissions()

            val location = locationViewModel.readLocationResult()

            if (location is ResolvedLocation) {
                val location2 = MyLocation(location.location.latitude, location.location.longitude)
                val pInfo = getAddressDescription(location2, true)
                loc.text = getString(
                    R.string.address_as_of_time_activity,
                    pInfo.locality,
                    getTimeAgo(location.location.time)
                )
                fajr.text = pInfo.fajr.toString().dropLast(3)
                sunrise.text = pInfo.sunrise.toString().dropLast(3)
                dhuhur.text = pInfo.dhuhur.toString().dropLast(3)
                asr.text = pInfo.asr.toString().dropLast(3)
                maghrib.text = pInfo.maghrib.toString().dropLast(3)
                isha.text = pInfo.isha.toString().dropLast(3)

                // DONE: save location in SharedPrefs
                val sharedPreference =  getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE)
                val editor = sharedPreference.edit()
                //editor.putFloat("lat", location2.latitude.toFloat())
                //editor.putFloat("Long", location2.longitude.toFloat())
                editor.putString("LT", location2.latitude.toString())
                editor.putString("LL", location2.longitude.toString())
                editor.apply()
            } else {
                loc.setText(R.string.location_error)
            }
        }

        forceComplicationUpdate()
    }

    override fun onStop() {
        forceComplicationUpdate()
        super.onStop()
    }

    suspend fun checkPermissions() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
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