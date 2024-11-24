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
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
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

class SalahComplicationRangeProviderService : SalahComplicationBaseProviderService() {

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
            ComplicationType.RANGED_VALUE -> getRangedValueComplicationData(currentPrayer)
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
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
        var text = PlainComplicationText.Builder(String.format("%s - %02d:%02d - %s", current, hour, minute, next)).build()
        //
        var emptyText = PlainComplicationText.Builder("").build()
        return RangedValueComplicationData.Builder(
            currentValue , 0F, maxValue, emptyText
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
            .setText(text)
            .build()
    }

}