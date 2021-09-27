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
package com.tazzix.wear.salah.data

import android.location.Location
import com.azan.Time

sealed class LocationResult {
    companion object
}

object PermissionError: LocationResult()

object NoLocation: LocationResult()

data class MyLocation(val latitude: Double, val longitude: Double, val method: Int, val asrCalc: Int)

data class PrayerInfo(val locality: String, val fajr: Time, val sunrise: Time, val dhuhur: Time, val asr: Time, val maghrib: Time, val isha: Time)

data class ResolvedLocation(val location: Location, val prayerInfo: PrayerInfo): LocationResult()
