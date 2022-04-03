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

        // DONE: if location not found, put tap target
        return if (lat!=0.0 && lon!=0.0) {
            val location = MyLocation(lat, lon, method, asrCalc)
            val pInfo = getAddressDescription(location, false)
            val now = LocalTime.now().minusMinutes(15)
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
            if (lineNum==1) {
                var pId = "F"
                var aId = "S"
                var next = pts[0]
                var after = pts[1]
                if (now>next) { next = pts[1]; after = pts[2]; pId = "S"; aId = "D" }
                if (now>next) { next = pts[2]; after = pts[3]; pId = "D"; aId = "A" }
                if (now>next) { next = pts[3]; after = pts[4]; pId = "A"; aId = "M" }
                if (now>next) { next = pts[4]; after = pts[5]; pId = "M"; aId = "I" }
                if (now>next) { next = pts[5]; after = pts[0]; pId = "I"; aId = "F" }
                if (now>next) { next = pts[0]; after = pts[1]; pId = "F"; aId = "S" }
                //locality = "$pId ${next.toString()}/^ ${pInfo.sunrise.toString().dropLast(3)}/v ${pInfo.maghrib.toString().dropLast(3)}"
                //locality = if (type==ComplicationType.LONG_TEXT) "$pId $next / $aId $after" else "$pId$next / $aId $after"
                locality = if (type==ComplicationType.LONG_TEXT) "$pId$next/$aId$after" else "$pId$next / $aId $after"
            } else {
                val rise = pts[1].toString().trimStart('0')
                locality = if (type==ComplicationType.LONG_TEXT) "^${rise}/v${pts[4]}" else "^ $rise / v ${pts[4]}"
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