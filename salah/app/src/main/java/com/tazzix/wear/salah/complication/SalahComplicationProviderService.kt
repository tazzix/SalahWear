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
import android.util.Log
import androidx.wear.complications.data.*
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.complications.datasource.ComplicationRequest
import com.azan.Time
import com.tazzix.wear.salah.R
import com.tazzix.wear.salah.SalahActivity.Companion.tapAction
import com.tazzix.wear.salah.data.*
import com.tazzix.wear.salah.getAddressDescription
import com.tazzix.wear.salah.kt.CoroutinesComplicationDataSourceService
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


data class CurrentPrayerInfo(var current: LocalTime, var next: LocalTime, var currentName: String, var nextName: String, var pInfo: PrayerInfo) {
}

class SalahComplicationProviderService : CoroutinesComplicationDataSourceService() {
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()
        locationViewModel = LocationViewModel(applicationContext)
    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(complicationRequest.complicationType)

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return toComplicationData(type)
    }

    private fun toComplicationData(
        type: ComplicationType
    ): ComplicationData {
        var currentPrayer = getCurrentPrayerInfo()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getAddressDescriptionText(1, type, currentPrayer),
                getAddressDescriptionText(2, type, currentPrayer),
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
                getAddressDescriptionText(1, type, currentPrayer),
                getAddressDescriptionText(1, type, currentPrayer)
            )
                .setTitle(getAddressDescriptionText(2, type, currentPrayer))
                .setTapAction(tapAction())
                .build()
            ComplicationType.RANGED_VALUE -> getRangedValueComplicationData(currentPrayer)
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    private fun getCurrentPrayerInfo(): CurrentPrayerInfo? {
        val sharedPreference =  getSharedPreferences("SALAH_LOCATION",Context.MODE_PRIVATE)
        val lat = sharedPreference.getString("LT", "0.00")?.toDouble()!!
        val lon = sharedPreference.getString("LL", "0.00")?.toDouble()!!
        val method = sharedPreference.getInt("METHOD", 0)
        val asrCalc = sharedPreference.getInt("ASR_CALC", 0)
        val dstOffset = sharedPreference.getInt("DST_OFFSET", 1)

        // DONE: if location not found, put tap target
        if (lat!=0.0 && lon!=0.0) {
            val location = MyLocation(lat, lon, method, asrCalc, dstOffset)
            val pInfo = getAddressDescription(location, false)
            val now = LocalTime.now()
            var pts = arrayOf(
                LocalTime.parse("00:00:00", DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.fajr.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.sunrise.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.dhuhur.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.asr.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.maghrib.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse(pInfo.isha.toString(), DateTimeFormatter.ofPattern("H:m:ss")),
                LocalTime.parse("23:59:59", DateTimeFormatter.ofPattern("H:m:ss")),
            )
            var current = pts[0]
            var next = pts[1]
            var currentName = "Midnight"
            var nextName = "Fajr"
            if (now.isAfter(next)) { current = pts[1]; next = pts[2]; currentName = "Fajr"; nextName = "Sunrise"}
            if (now.isAfter(next)) { current = pts[2]; next = pts[3]; currentName = "Sunrise"; nextName = "Dhuhur"}
            if (now.isAfter(next)) { current = pts[3]; next = pts[4]; currentName = "Dhuhur"; nextName = "Asr"}
            if (now.isAfter(next)) { current = pts[4]; next = pts[5]; currentName = "Asr"; nextName = "Mahgrib"}
            if (now.isAfter(next)) { current = pts[5]; next = pts[6]; currentName = "Mahrib"; nextName = "Isha"}
            if (now.isAfter(next)) { current = pts[6]; next = pts[7]; currentName = "Isha"; nextName = "Midnight"}
            return CurrentPrayerInfo(current, next, currentName, nextName, pInfo)
        }
        return null;
    }

    private fun getRangedValueComplicationData(currentPrayerInfo: CurrentPrayerInfo?): RangedValueComplicationData {
        var currentValue = 5F
        var maxValue = 10F
        var current = LocalTime.now()
        var next = LocalTime.now()

        // DONE: if location not found, put tap target
        if (currentPrayerInfo != null) {
            current = currentPrayerInfo.current
            next = currentPrayerInfo.next
            val now = LocalTime.now()
            var offset = current.toSecondOfDay() / 60F
            currentValue = (now.toSecondOfDay() / 60F) - offset
            maxValue = (next.toSecondOfDay() / 60F) - offset
        }
        Log.i("debug", "${currentValue} - ${maxValue}")
        var delta = maxValue - currentValue
        var hour = (delta / 60F).toInt()
        var minute = (delta % 60F).toInt()
        return RangedValueComplicationData.Builder(
            currentValue , 0F, maxValue,
            PlainComplicationText.Builder("").build()
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
            .setText(PlainComplicationText.Builder(String.format("%02d:%02d - %02d:%02d - %02d:%02d", current.hour, current.minute, hour, minute, next.hour, next.minute)).build())
            .build()
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

    private fun getAddressDescriptionText(lineNum: Int, type: ComplicationType, currentPrayerInfo: CurrentPrayerInfo?): ComplicationText {
        val locality: String
        if (currentPrayerInfo == null) {
            locality = getString(R.string.no_location)
        } else {
            if (lineNum==1) {
                var currentName = currentPrayerInfo.currentName[0]
                var nextName = currentPrayerInfo.nextName[0]
                var next = currentPrayerInfo.current
                var after = currentPrayerInfo.next
                locality = if (type==ComplicationType.LONG_TEXT) "$currentName$next/$nextName$after" else "$currentName$next / $nextName $after"
            } else {
                locality = if (type==ComplicationType.LONG_TEXT) "^${currentPrayerInfo.pInfo.sunrise}/v${currentPrayerInfo.pInfo.maghrib}" else "^ ${currentPrayerInfo.pInfo.sunrise} / v ${currentPrayerInfo.pInfo.maghrib}"
            }
        }
        return PlainComplicationText.Builder(locality).build()
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