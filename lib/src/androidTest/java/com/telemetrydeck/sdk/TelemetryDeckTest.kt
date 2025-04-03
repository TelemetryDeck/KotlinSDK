package com.telemetrydeck.sdk


import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.providers.DurationSignalTrackerProvider
import com.telemetrydeck.sdk.providers.SessionTrackingSignalProvider
import com.telemetrydeck.sdk.signals.Acquisition
import com.telemetrydeck.sdk.signals.Session
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL
import java.util.Date
import java.util.UUID
import kotlin.collections.find
import kotlin.text.startsWith


@RunWith(AndroidJUnit4::class)
class TelemetryDeckTest {

    @Before
    @After
    @UiThreadTest
    fun cleanup() {
        TelemetryDeck.stop()
        // make sure all tests run as a new installation
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        listOf("telemetrydeckduration", "telemetrydeckstracking", "telemetrydeckid").forEach {
            val file = File(appContext.filesDir, it)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    @UiThreadTest
    @Test
    fun first_start_sends_acquisition_new_install_detected_signal() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionProvider(sut)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()

        // act
        sut.onStart(lifecycleOwner)

        verify {
            signalCache.add(withArg {
                assertEquals(Acquisition.NewInstallDetected.signalName, it.type)
                assertNotNull(it.sessionID)
            })
        }
    }

    @UiThreadTest
    @Test
    fun first_start_sends_new_session_signal() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionProvider(sut)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()

        // act
        sut.onStart(lifecycleOwner)

        verify {
            signalCache.add(withArg {
                assertEquals(Session.Started.signalName, it.type)
                assertNotNull(it.sessionID)
            })
        }
    }

    @UiThreadTest
    @Test
    fun first_start_accepts_starting_session_id() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        val sessionID = UUID.randomUUID()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionID(sessionID)
                .sessionProvider(sut)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()

        // act
        sut.onStart(lifecycleOwner)

        verify(exactly = 2) { // both new installation and new session
            signalCache.add(withArg {
                assertEquals(sessionID.toString(), it.sessionID)
            })
        }
    }

    @UiThreadTest
    @Test
    fun does_not_send_new_session_signal_if_disabled() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionProvider(sut)
                .sendNewSessionBeganSignal(false)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()

        // act
        sut.onStart(lifecycleOwner)

        verify(inverse = true) {
            signalCache.add(withArg {
                assertEquals(Session.Started.signalName, it.type)
            })
        }
    }

    @UiThreadTest
    @Test
    fun newSession_resets_sessionID_and_emits_signal() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        val sessionID = UUID.randomUUID()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionID(sessionID)
                .sessionProvider(sut)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        sut.onStart(lifecycleOwner)

        // act
        TelemetryDeck.newSession()

        verify {
            signalCache.add(withArg {
                assertEquals(Session.Started.signalName, it.type)
                assertNotEquals(sessionID.toString(), it.sessionID)
            })
        }
    }

    @UiThreadTest
    @Test
    fun newSession_resets_sessionID_to_preferred_id_and_emits_signal() {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = SessionTrackingSignalProvider()
        TelemetryDeck.start(
            ApplicationProvider.getApplicationContext<Application>(),
            prepareBuilder()
                .signalCache(signalCache)
                .sessionProvider(sut)
        )
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        sut.onStart(lifecycleOwner)
        val sessionID = UUID.randomUUID()

        // act
        TelemetryDeck.newSession(sessionID)

        verify {
            signalCache.add(withArg {
                assertEquals(Session.Started.signalName, it.type)
                assertEquals(sessionID.toString(), it.sessionID)
            })
        }
    }


    @UiThreadTest
    @Test
    fun signal_sets_testMode_and_sessionID() {
        val sessionID = UUID.randomUUID()
        val signalCache = startTelemetryDeck(
            prepareBuilder()
                .sessionID(sessionID)
                .testMode(false)
        )
        // act
        TelemetryDeck.signal("type", "clientUser", emptyMap())


        verify {
            signalCache.add(withArg {
                assertEquals("type", it.type)
                assertEquals(sessionID.toString(), it.sessionID)
                assertEquals(
                    "6721870580401922549fe8fdb09a064dba5b8792fa018d3bd9ffa90fe37a0149",
                    it.clientUser
                )
                assertEquals("false", it.isTestMode)
            })
        }
    }

    @UiThreadTest
    @Test
    fun signal_sets_session_tracking_parameters() {
        val sessionID = UUID.randomUUID()
        val signalCache = startTelemetryDeck(
            prepareBuilder()
                .sessionID(sessionID)
                .testMode(false)
        )

        // simulate going into the background 10 minutes later
        (TelemetryDeck.instance!!.sessionManager as SessionTrackingSignalProvider).handleOnBackground(
            Date(Date().time + 1000 * 10 * 60)
        )

        // and back to foreground an hour later
        (TelemetryDeck.instance!!.sessionManager as SessionTrackingSignalProvider).handleOnForeground(
            Date(Date().time + 1000 * 60 * 60)
        )

        // act
        TelemetryDeck.signal("type", "clientUser", emptyMap())


        verify {
            signalCache.add(withArg {
                assertEquals("type", it.type)
                assertNotEquals(sessionID.toString(), it.sessionID)
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Acquisition.firstSessionDate:") })
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Retention.averageSessionSeconds:") })
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Retention.distinctDaysUsed:") })
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Retention.totalSessionsCount:") })
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Retention.previousSessionSeconds:") })
                assertNotNull(it.payload.find { it.startsWith("TelemetryDeck.Retention.distinctDaysUsedLastMonth:") })
            })
        }
    }

    @UiThreadTest
    @Test
    fun signal_sets_calendar_parameters() {
        val sessionID = UUID.randomUUID()
        val signalCache = startTelemetryDeck(
            prepareBuilder()
                .sessionID(sessionID)
        )

        // act
        TelemetryDeck.signal("type", "clientUser", emptyMap())


        verify {
            signalCache.add(withArg { signal ->
                assertEquals("type", signal.type)
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.dayOfMonth:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.dayOfWeek:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.dayOfYear:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.weekOfYear:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.isWeekend:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.monthOfYear:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.quarterOfYear:") })
                assertNotNull(signal.payload.find { it.startsWith("TelemetryDeck.Calendar.hourOfDay:") })
            })
        }
    }

    @UiThreadTest
    @Test
    fun allows_for_duration_tracking() {
        val signalCache = startTelemetryDeck(
            prepareBuilder()
        )

        // act
        TelemetryDeck.startDurationSignal("type")
        TelemetryDeck.stopAndSendDurationSignal("type")


        verify {
            signalCache.add(withArg {
                assertEquals("type", it.type)
                val duration =
                    it.payload.find { it.startsWith("TelemetryDeck.Signal.durationInSeconds:") }
                assertNotNull(duration)
            })
        }
    }

    @UiThreadTest
    @Test
    fun allows_for_duration_tracking_with_floatvalue_customer_id() {
        val signalCache = startTelemetryDeck(
            prepareBuilder()
        )

        // act
        TelemetryDeck.startDurationSignal("type")
        TelemetryDeck.stopAndSendDurationSignal("type", floatValue = 10.0, customUserID = "user")


        verify {
            signalCache.add(withArg {
                assertEquals("type", it.type)
                val duration =
                    it.payload.find { it.startsWith("TelemetryDeck.Signal.durationInSeconds:") }
                assertNotNull(duration)
                assertEquals(10.0, it.floatValue)
                assertEquals(
                    "04f8996da763b7a969b1028ee3007569eaf3a635486ddab211d512c85b9df8fb",
                    it.clientUser
                )
            })
        }
    }

    @UiThreadTest
    @Test
    fun duration_tracking_with_and_without_background_time() {
        startTelemetryDeck(
            prepareBuilder()
                .testMode(false)
        )

        val sut =
            (TelemetryDeck.instance!!.providers.find { it is DurationSignalTrackerProvider } as DurationSignalTrackerProvider)

        val now = Date()
        sut.handleOnForeground(now)

        sut.startTracking("foreground", emptyMap(), includeBackgroundTime = false, now)
        sut.startTracking("foreground_and_background", emptyMap(), includeBackgroundTime = true, now)

        // +600seconds, simulate going into the background 10 minutes later
        val time1 = Date(now.time + 1000 * 10 * 60)
        sut.handleOnBackground(
            Date(now.time + 1000 * 10 * 60)
        )

        // +3600seconds, and back to foreground an hour later
        val endTime = Date(time1.time + (1000 * 60 * 60))

        sut.handleOnForeground(
            endTime
        )

        val paramsForeground = sut.stopTracking("foreground", emptyMap(), endTime)
        val paramsForeAndBackground =
            sut.stopTracking("foreground_and_background", emptyMap(), endTime)

        assertNotNull(paramsForeground)
        assertNotNull(paramsForeAndBackground)

        assertEquals("600.000", paramsForeground?.get("TelemetryDeck.Signal.durationInSeconds"))
        assertEquals("4200.000", paramsForeAndBackground?.get("TelemetryDeck.Signal.durationInSeconds"))
    }


    private fun prepareBuilder(): TelemetryDeck.Builder {
        val httpClient = mockk<TelemetryApiClient>()
        val factory = MockApiFactory(httpClient)
        return TelemetryDeck.Builder()
            .apiClientFactory(factory)
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
    }

    private fun startTelemetryDeck(builder: TelemetryDeck.Builder): SignalCache {
        val signalCache = mockk<SignalCache>()
        every { signalCache.add(any()) } returns Unit
        val sut = builder
            .signalCache(signalCache)
            .build(ApplicationProvider.getApplicationContext<Application>())
        TelemetryDeck.instance = sut

        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        for (provider in sut.providers) {
            if (provider is DefaultLifecycleObserver) {
                provider.onStart(lifecycleOwner)
            }
        }
        (sut.sessionManager as SessionTrackingSignalProvider).onStart(lifecycleOwner)
        return signalCache
    }
}

class MockApiFactory(private val client: TelemetryApiClient) : TelemetryApiClientFactory {
    override fun create(
        apiBaseURL: URL,
        showDebugLogs: Boolean,
        namespace: String?,
        logger: DebugLogger?
    ): TelemetryApiClient {
        return client
    }
}