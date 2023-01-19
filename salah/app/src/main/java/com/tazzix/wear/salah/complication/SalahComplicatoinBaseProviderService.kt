package com.tazzix.wear.salah.complication

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.complications.datasource.ComplicationRequest
import com.tazzix.wear.salah.R
import com.tazzix.wear.salah.data.LocationViewModel
import com.tazzix.wear.salah.data.MyLocation
import com.tazzix.wear.salah.getAddressDescription
import com.tazzix.wear.salah.kt.CoroutinesComplicationDataSourceService
import java.time.LocalTime
import java.time.format.DateTimeFormatter

abstract class SalahComplicationBaseProviderService : CoroutinesComplicationDataSourceService() {
    private lateinit var locationViewModel: LocationViewModel
    override fun onCreate() {
        super.onCreate()
        locationViewModel = LocationViewModel(applicationContext)
    }



    protected fun getCurrentPrayerInfo(): CurrentPrayerInfo? {
        val sharedPreference =  getSharedPreferences("SALAH_LOCATION", Context.MODE_PRIVATE)
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

    protected fun getAddressDescriptionText(lineNum: Int, type: ComplicationType, currentPrayerInfo: CurrentPrayerInfo?): ComplicationText {
        val locality: String
        if (currentPrayerInfo == null) {
            locality = getString(R.string.no_location)
        } else {
            if (lineNum==1) {
                var currentName = currentPrayerInfo.currentName[0]
                var nextName = currentPrayerInfo.nextName[0]
                var next = currentPrayerInfo.current
                var after = currentPrayerInfo.next
                locality = if (type== ComplicationType.LONG_TEXT) "$currentName$next/$nextName$after" else "$currentName$next / $nextName $after"
            } else {
                locality = if (type== ComplicationType.LONG_TEXT) "^${currentPrayerInfo.pInfo.sunrise}/v${currentPrayerInfo.pInfo.maghrib}" else "^ ${currentPrayerInfo.pInfo.sunrise} / v ${currentPrayerInfo.pInfo.maghrib}"
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

