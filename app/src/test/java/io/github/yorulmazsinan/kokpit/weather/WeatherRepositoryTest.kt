package io.github.yorulmazsinan.kokpit.weather

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val HOUR = 3_600_000L

class WeatherRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val server = MockWebServer()
    private var now = 1_800_000_000_000L

    @Before fun setUp() = server.start()
    @After fun tearDown() = server.close()

    private fun TestScope.repository(location: Pair<Double, Double>? = 41.0 to 29.0): WeatherRepository {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "weather.preferences_pb") }
        val client = OpenMeteoClient(OkHttpClient(), baseUrl = server.url("/").toString().trimEnd('/'))
        return WeatherRepository(client, store, location = { location }, clock = { now })
    }

    @Test
    fun returnsCachedWeatherWhenNetworkFails() = runTest {
        val repo = repository()
        server.enqueue(MockResponse.Builder().body(sampleJson()).build())
        repo.refresh()
        val first = repo.state.value
        assertTrue(first is WeatherState.Fresh)

        server.close()
        now += HOUR
        repo.refresh()

        val second = repo.state.value
        assertTrue(second is WeatherState.Fresh)
        assertEquals((first as WeatherState.Fresh).weather, (second as WeatherState.Fresh).weather)
    }

    @Test
    fun marksStaleAfterThreeHours() = runTest {
        val repo = repository()
        server.enqueue(MockResponse.Builder().body(sampleJson()).build())
        repo.refresh()
        server.close()
        now += 3 * HOUR + 1
        repo.refresh()
        assertTrue(repo.state.value is WeatherState.Stale)
    }

    @Test
    fun clockBehindFetchTimeIsStale() = runTest {
        val repo = repository()
        server.enqueue(MockResponse.Builder().body(sampleJson()).build())
        repo.refresh()
        server.close()
        now = 1_170_000_000_000L
        repo.refresh()
        assertTrue(repo.state.value is WeatherState.Stale)
    }

    @Test
    fun noLocationKeepsNoData() = runTest {
        val repo = repository(location = null)
        repo.refresh()
        assertEquals(WeatherState.NoData, repo.state.value)
        assertEquals(0, server.requestCount)
    }
}
