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
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


class SalahComplicationProviderService : SalahComplicationBaseProviderService() {
    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(complicationRequest.complicationType)

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return toComplicationData(type)
    }
    private fun toComplicationData(
        type: ComplicationType
    ): ComplicationData {
        val currentPrayer = getCurrentPrayerInfo()
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

}