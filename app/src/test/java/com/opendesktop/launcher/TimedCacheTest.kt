package com.opendesktop.launcher

import com.opendesktop.launcher.data.TimedCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class TimedCacheTest {
    @Test
    fun parallelRequestsShareOneLoad() = runBlocking {
        val cache = TimedCache<Int>(60_000)
        val gate = CompletableDeferred<Unit>()
        var loads = 0
        val results = List(5) {
            async { cache.get("warszawa") { loads++; gate.await(); 42 } }
        }
        yield() // wszystkie zapytania czekają już na pierwsze ładowanie
        gate.complete(Unit)
        assertEquals(List(5) { 42 }, results.awaitAll())
        assertEquals(1, loads)
    }

    @Test
    fun expiredValueIsLoadedAgain() = runBlocking {
        var time = 0L
        val cache = TimedCache<Int>(1_000) { time }
        var loads = 0
        cache.get("k") { ++loads }
        time = 500
        assertEquals(1, cache.get("k") { ++loads })
        time = 1_500
        assertEquals(2, cache.get("k") { ++loads })
    }
}
