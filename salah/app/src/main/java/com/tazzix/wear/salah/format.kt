// Copyright 2021 Google LLC
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

import android.content.Context
import android.location.Geocoder
import android.text.format.DateUtils
import com.azan.*
import com.azan.astrologicalCalc.Location
import com.azan.astrologicalCalc.SimpleDate
import com.azan.astrologicalCalc.Utils
import com.tazzix.wear.salah.data.MyLocation
import com.tazzix.wear.salah.data.PrayerInfo
import java.util.*

fun getTimeAgo(time: Long): CharSequence {
    return DateUtils.getRelativeTimeSpanString(time)
}

fun Context.getAddressDescription(location: MyLocation, resolveLocality: Boolean): PrayerInfo {
    var locationVal = "N/A"
    if (resolveLocality) {
        try {
            val list = Geocoder(this).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (list != null) {
                locationVal = list[0].locality
            }
        } catch (e: Exception) {
        }
    }
    if (locationVal.isEmpty()) locationVal = String.format("%.3f, %.3f", location.latitude, location.longitude)

    val today = SimpleDate(GregorianCalendar())
    // Subtract 1 from DST due to bug in library: https://github.com/ahmedeltaher/Prayer-Times-Android-Azan/issues/27#issue-866377567
    val loc = Location(location.latitude, location.longitude, TimeZone.getDefault().rawOffset.toDouble()/3600000, TimeZone.getDefault().dstSavings/3600000 - 1)
    val method = when(location.method) {
        0 -> Method.KARACHI_HANAF
        1 -> Method.UMM_ALQURRA
        2 -> Method.MUSLIM_LEAGUE
        3 -> Method.EGYPT_SURVEY
        4 -> Method.NORTH_AMERICA
        // FRANCE
        5 -> Method(12.0, 12.0, Utils.DEF_IMSAAK_ANGLE, 0, 0, 0,
            Rounding.SPECIAL, Madhhab.HANAFI, Utils.DEF_NEAREST_LATITUDE, ExtremeLatitude.GOOD_INVALID, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        // KUWAIT
        6 -> Method(18.0, 17.5, Utils.DEF_IMSAAK_ANGLE, 0, 0, 0,
            Rounding.SPECIAL, Madhhab.HANAFI, Utils.DEF_NEAREST_LATITUDE, ExtremeLatitude.GOOD_INVALID, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        // ITHNA ASHARI
        7 -> Method(16.0, 14.0, Utils.DEF_IMSAAK_ANGLE, 0, 0, 0,
            Rounding.SPECIAL, Madhhab.HANAFI, Utils.DEF_NEAREST_LATITUDE, ExtremeLatitude.GOOD_INVALID, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        // TEHRAN UNIVERSITY
        8 -> Method(17.7, 14.0, Utils.DEF_IMSAAK_ANGLE, 0, 0, 0,
            Rounding.SPECIAL, Madhhab.HANAFI, Utils.DEF_NEAREST_LATITUDE, ExtremeLatitude.GOOD_INVALID, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        else -> Method.KARACHI_HANAF
    }
    method.madhhab = if(location.asrCalc==0) Madhhab.HANAFI else Madhhab.SHAAFI
    val azan = Azan(loc, method)
    val ptimes = azan.getPrayerTimes(today)

    return PrayerInfo(locationVal, ptimes.fajr(), ptimes.shuruq(), ptimes.thuhr(), ptimes.assr(), ptimes.maghrib(), ptimes.ishaa())
}

/*fun describeLocation(location: LocationResult): String {
    return when (location) {
        //is ResolvedLocation -> getAddressDescription(location).locality
        else -> "Unknown"
    }
}*/
