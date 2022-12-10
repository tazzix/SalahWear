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
package com.tazzix.wear.salah.complication

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.location.Address
import android.location.Location
import android.location.LocationManager
import androidx.wear.complications.data.*
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.complications.datasource.ComplicationRequest
import com.tazzix.wear.salah.R
import com.tazzix.wear.salah.SalahActivity.Companion.tapAction
import com.tazzix.wear.salah.data.*
import com.tazzix.wear.salah.getAddressDescription
import com.tazzix.wear.salah.kt.CoroutinesComplicationDataSourceService
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class SalahComplicationProviderService : CoroutinesComplicationDataSourceService() {
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()
        locationViewModel = LocationViewModel(applicationContext)
    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(complicationRequest.complicationType)//, locationViewModel.readLocationResult())

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.longitude = 0.0
        location.latitude = 0.0
        val address = Address(Locale.ENGLISH)
        address.countryName = "Null Island"

        return toComplicationData(type)
    }

    private fun toComplicationData(
        type: ComplicationType
    ): ComplicationData {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getAddressDescriptionText(1, type),
                getAddressDescriptionText(2, type),
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_launcher
                        )
                    ).build()
                )
                .setTapAction(tapAction())
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                getAddressDescriptionText(1, type),
                getAddressDescriptionText(1, type)
            )
                .setTitle(getAddressDescriptionText(2, type))
                /*.setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_launcher
                        )
                    ).build()
                )*/
                .setTapAction(tapAction())
                .build()
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    private fun getTimeAgoComplicationText(fromTime: Long): TimeDifferenceComplicationText.Builder {
        return TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            CountUpTimeReference(fromTime)
        ).apply {
            setMinimumTimeUnit(TimeUnit.MINUTES)
            setDisplayAsNow(true)
        }
    }

    private fun getAddressDescriptionText(lineNum: Int, type: ComplicationType): ComplicationText {
        // DONE: read location from shared prefs
        val sharedPreference =  getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE)
        val lat = sharedPreference.getString("LT", "0.00")?.toDouble()!!
        val lon = sharedPreference.getString("LL", "0.00")?.toDouble()!!
        val method = sharedPreference.getInt("METHOD", 0)
        val asrCalc = sharedPreference.getInt("ASR_CALC", 0)
        val dstOffset = sharedPreference.getInt("DST_OFFSET", 1)

        // DONE: if location not found, put tap target
        return if (lat!=0.0 && lon!=0.0) {
            val location = MyLocation(lat, lon, method, asrCalc, dstOffset)
            val pInfo = getAddressDescription(location, false)
            val now = LocalTime.now()
            var pts = arrayOf(
                LocalTime.parse(pInfo.fajr.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.sunrise.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.dhuhur.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.asr.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.maghrib.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.isha.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
            )
            // DateFormat.getTimeFormat(context); if changing from hard 24h format to whatever user has set

            val locality: String
            var pId = "Fajr"
            var aId = "Sunrise"
            var currPrayer = pts[0]
            var nextPrayer = pts[1]
            if (now>nextPrayer) { currPrayer = pts[1]; nextPrayer = pts[2]; pId = "Sunrise"; aId = "Dhuhr" }
            if (now>nextPrayer) { currPrayer = pts[2]; nextPrayer = pts[3]; pId = "Dhuhr"; aId = "Asr" }
            if (now>nextPrayer) { currPrayer = pts[3]; nextPrayer = pts[4]; pId = "Asr"; aId = "Maghrib" }
            if (now>nextPrayer) { currPrayer = pts[4]; nextPrayer = pts[5]; pId = "Maghrib"; aId = "Isha" }
            if (now>nextPrayer || now < pts[0]) { currPrayer = pts[5]; nextPrayer = pts[0]; pId = "Isha"; aId = "Fajr" }
            locality = if (lineNum==1) {
                val nextStr = currPrayer.format(DateTimeFormatter.ofPattern("h:mma"))
                val afterStr = nextPrayer.format(DateTimeFormatter.ofPattern("h:mma"))
                if (type==ComplicationType.LONG_TEXT) "${pId.get(0)} $nextStr / ${aId.get(0)} $afterStr" else "$pId ${nextStr} / $aId $afterStr"
            } else {
                val remainingSeconds = now.until(nextPrayer, ChronoUnit.SECONDS)
                val remainingTime = (LocalTime.MIDNIGHT.plusSeconds(remainingSeconds))
                "$aId in ${remainingTime.hour}h ${remainingTime.minute}m"
            }

            return PlainComplicationText.Builder(locality).build()
        } else {
            PlainComplicationText.Builder(getString(R.string.no_location)).build()
        }
    }

    companion object {
        fun Context.forceComplicationUpdate() {
            if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val request = ComplicationDataSourceUpdateRequester(
                    applicationContext, ComponentName(
                        applicationContext, SalahComplicationProviderService::class.java
                    )
                )
                request.requestUpdateAll()
            }
        }
    }
}