package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.platform.getCurrentLocale
import java.lang.ref.WeakReference
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

        return enrichInternal(additionalPayload, Calendar.getInstance())
    }

    internal fun enrichInternal(additionalPayload: Map<String, String>, now: Calendar): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        val isoDayOfWeek = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        val monthOfYear = now.get(Calendar.MONTH) + 1
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfMonth.paramName] = "${now.get(Calendar.DAY_OF_MONTH)}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfWeek.paramName] = "$isoDayOfWeek"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.DayOfYear.paramName] = "${now.get(Calendar.DAY_OF_YEAR)}"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.MonthOfYear.paramName] = "$monthOfYear"
        signalPayload[com.telemetrydeck.sdk.params.Calendar.HourOfDay.paramName] = "${now.get(Calendar.HOUR_OF_DAY) + 1}"

        // Note: isWeekend only accounts for Sat-Sun counties
        signalPayload[com.telemetrydeck.sdk.params.Calendar.IsWeekend.paramName] = "${isoDayOfWeek in listOf(6, 7)}"

        signalPayload[com.telemetrydeck.sdk.params.Calendar.WeekOfYear.paramName] = "${now.get(Calendar.WEEK_OF_YEAR)}"

        val quarterNumber = (monthOfYear + 2) / 3
        signalPayload[com.telemetrydeck.sdk.params.Calendar.QuarterOfYear.paramName] = "$quarterNumber"

        return signalPayload
    }
}