package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.params.Acquisition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Clock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionTrackingConcurrencyTest {

    private val dayPattern = Regex("""^\d{4}-\d{2}-\d{2}$""")

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun enrich_survives_concurrent_foreground_and_background_transitions() = runBlocking {
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(RuntimeEnvironment.getApplication())
        val sut = manager.sessionManager as SessionTrackingSignalProvider
        sut.handleOnForeground()

        val errors = ConcurrentLinkedQueue<Throwable>()
        val threadCount = 8
        val iterations = 100

        newFixedThreadPoolContext(threadCount, "session-tracking-stress").use { dispatcher ->
            CoroutineScope(dispatcher).launch {
                val jobs = (0 until threadCount).map { threadIndex ->
                    launch {
                        repeat(iterations) { i ->
                            try {
                                val now = Clock.System.now()
                                if ((threadIndex + i) % 2 == 0) {
                                    sut.handleOnBackground(now)
                                } else {
                                    sut.handleOnForeground(now)
                                }
                                val attributes = sut.enrich("signal", "clientUser", emptyMap())
                                val firstSessionDate = attributes[Acquisition.FirstSessionDate.paramName]
                                if (firstSessionDate != null && firstSessionDate.isNotEmpty() && !dayPattern.matches(firstSessionDate)) {
                                    throw AssertionError("malformed firstSessionDate: $firstSessionDate")
                                }
                            } catch (t: Throwable) {
                                errors.add(t)
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }
            }.join()
        }

        assertTrue(
            "Concurrent foreground/background/enrich failed with ${errors.size} errors, first: ${errors.firstOrNull()}",
            errors.isEmpty()
        )

        // the provider must still be in a well-formed, queryable state afterwards
        val finalAttributes = sut.enrich("signal", "clientUser", emptyMap())
        val finalFirstSessionDate = finalAttributes[Acquisition.FirstSessionDate.paramName]
        assertTrue(
            "expected a well-formed yyyy-MM-dd firstSessionDate, got $finalFirstSessionDate",
            finalFirstSessionDate != null && dayPattern.matches(finalFirstSessionDate)
        )
    }
}
