package com.telemetrydeck.sdk.providers

import android.content.Context
import android.os.Build
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.platform.getCurrentLocale
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Calendar

class CalendarParameterProvider : TelemetryDeckProvider {
    private var app: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.app = WeakReference(ctx)
        this.manager = WeakReference(client)
    }

    override fun stop() {
        // nothing to do
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val context = app?.get()

        if (context == null) {
            manager?.get()?.debugLogger?.error("CalendarParameterProvider: context is null")
            return additionalPayload
        }

        val userLocale = getCurrentLocale(context, manager?.get()?.debugLogger)
        if (userLocale == null) {
            manager?.get()?.debugLogger?.error("CalendarParameterProvider: Locale is null")
        }

        val signalPayload = additionalPayload.toMutableMap()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfMonth.paramName] = "${now.dayOfMonth}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfWeek.paramName] = "${now.dayOfWeek.isoDayNumber}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfYear.paramName] = "${now.dayOfYear}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.MonthOfYear.paramName] = "${now.monthNumber}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.HourOfDay.paramName] = "${now.hour}"

        // Note: isWeekend only accounts for Sat-Sun counties
        signalPayload[com.telemetrydeck.sdk.params.Calendar.IsWeekend.paramName] = "${now.dayOfWeek.isoDayNumber in listOf(6, 7)}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentDate = LocalDate.now()
            signalPayload[com.telemetrydeck.sdk.params.Calendar.WeekOfYear.paramName] = "${Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)}"
            signalPayload[com.telemetrydeck.sdk.params.Calendar.QuarterOfYear.paramName] = "${currentDate.get(IsoFields.QUARTER_OF_YEAR)}"
        } else {
            // falling back to simple 3 month approach
            val quarterNumber = when (now.monthNumber) {
                in 1..3 -> 1
                in 4..6 -> 2
                in 7..9 -> 3
                in 10..12 -> 4
                else -> null
            }
            if (quarterNumber != null) {
                signalPayload[com.telemetrydeck.sdk.params.Calendar.QuarterOfYear.paramName] = "$quarterNumber"
            }
        }

        return signalPayload
    }
}