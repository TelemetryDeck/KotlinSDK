package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.params.Calendar as CalendarParams
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalendarParameterProviderTest {

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Calendar {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(year, month - 1, day, hour, minute, second)
        return calendar
    }

    @Test
    fun enriches_a_wednesday_with_iso_day_numbering() {
        // 2025-01-15 is a Wednesday (ISO day 3)
        val sut = CalendarParameterProvider()

        val attributes = sut.enrichInternal(emptyMap(), calendar(2025, 1, 15, 10, 30, 0))

        assertEquals("15", attributes[CalendarParams.DayOfMonth.paramName])
        assertEquals("3", attributes[CalendarParams.DayOfWeek.paramName])
        assertEquals("15", attributes[CalendarParams.DayOfYear.paramName])
        assertEquals("1", attributes[CalendarParams.MonthOfYear.paramName])
        assertEquals("11", attributes[CalendarParams.HourOfDay.paramName])
        assertEquals("false", attributes[CalendarParams.IsWeekend.paramName])
        // 3 under the default-locale week rules (java.util.Calendar.WEEK_OF_YEAR)
        assertEquals("3", attributes[CalendarParams.WeekOfYear.paramName])
    }

    @Test
    fun enriches_a_sunday_as_iso_day_seven_and_weekend() {
        // 2025-01-19 is a Sunday (ISO day 7)
        val sut = CalendarParameterProvider()

        val attributes = sut.enrichInternal(emptyMap(), calendar(2025, 1, 19, 23, 0, 0))

        assertEquals("7", attributes[CalendarParams.DayOfWeek.paramName])
        assertEquals("true", attributes[CalendarParams.IsWeekend.paramName])
        assertEquals("24", attributes[CalendarParams.HourOfDay.paramName])
    }

    @Test
    fun enriches_a_saturday_as_iso_day_six_and_weekend() {
        // 2025-01-18 is a Saturday (ISO day 6)
        val sut = CalendarParameterProvider()

        val attributes = sut.enrichInternal(emptyMap(), calendar(2025, 1, 18, 12, 0, 0))

        assertEquals("6", attributes[CalendarParams.DayOfWeek.paramName])
        assertEquals("true", attributes[CalendarParams.IsWeekend.paramName])
    }

    @Test
    fun enriches_a_monday_as_iso_day_one_and_not_weekend() {
        // 2025-01-13 is a Monday (ISO day 1)
        val sut = CalendarParameterProvider()

        val attributes = sut.enrichInternal(emptyMap(), calendar(2025, 1, 13, 0, 0, 0))

        assertEquals("1", attributes[CalendarParams.DayOfWeek.paramName])
        assertEquals("false", attributes[CalendarParams.IsWeekend.paramName])
        assertEquals("1", attributes[CalendarParams.HourOfDay.paramName])
    }

    @Test
    fun preserves_additional_payload_entries() {
        val sut = CalendarParameterProvider()

        val attributes = sut.enrichInternal(mapOf("custom" to "value"), calendar(2025, 6, 1, 8, 0, 0))

        assertEquals("value", attributes["custom"])
        assertEquals("6", attributes[CalendarParams.MonthOfYear.paramName])
    }

    @Test
    fun day_of_week_mapping_covers_all_seven_java_calendar_values() {
        // java.util.Calendar.DAY_OF_WEEK: Sunday=1 .. Saturday=7
        // ISO day numbering used by the SDK: Monday=1 .. Sunday=7
        val expected = mapOf(
            Calendar.SUNDAY to 7,
            Calendar.MONDAY to 1,
            Calendar.TUESDAY to 2,
            Calendar.WEDNESDAY to 3,
            Calendar.THURSDAY to 4,
            Calendar.FRIDAY to 5,
            Calendar.SATURDAY to 6,
        )
        val sut = CalendarParameterProvider()

        for ((calendarDayOfWeek, isoDayOfWeek) in expected) {
            // 2025-01-12 is a Sunday; offset from there to land on the target day of week
            val instant = calendar(2025, 1, 12, 12, 0, 0)
            val offset = (calendarDayOfWeek - Calendar.SUNDAY + 7) % 7
            instant.add(Calendar.DAY_OF_MONTH, offset)

            val attributes = sut.enrichInternal(emptyMap(), instant)

            assertEquals(
                "Calendar.DAY_OF_WEEK=$calendarDayOfWeek must map to ISO day $isoDayOfWeek",
                "$isoDayOfWeek",
                attributes[CalendarParams.DayOfWeek.paramName]
            )
        }
    }

    @Test
    fun quarter_of_year_mapping_covers_all_twelve_months() {
        val expected = mapOf(
            1 to 1, 2 to 1, 3 to 1,
            4 to 2, 5 to 2, 6 to 2,
            7 to 3, 8 to 3, 9 to 3,
            10 to 4, 11 to 4, 12 to 4,
        )
        val sut = CalendarParameterProvider()

        for ((month, quarter) in expected) {
            val attributes = sut.enrichInternal(emptyMap(), calendar(2025, month, 15, 10, 30, 0))

            assertEquals(
                "month=$month must map to quarter $quarter",
                "$quarter",
                attributes[CalendarParams.QuarterOfYear.paramName]
            )
        }
    }
}
