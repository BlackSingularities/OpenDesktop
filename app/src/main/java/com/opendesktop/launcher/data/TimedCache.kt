package com.opendesktop.launcher.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Pamięć wyników z czasem ważności i jednym zapytaniem naraz dla klucza: gdy kilka widżetów
 * prosi o tę samą prognozę równocześnie, sieć dostaje jedno zapytanie, a reszta czeka na jego wynik.
 */
class TimedCache<V>(private val ttlMs: Long, private val now: () -> Long = System::currentTimeMillis) {
    private val values = ConcurrentHashMap<String, Pair<Long, V>>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    fun fresh(key: String): V? = values[key]?.takeIf { now() - it.first < ttlMs }?.second

    suspend fun get(key: String, load: suspend () -> V): V {
        fresh(key)?.let { return it }
        return locks.getOrPut(key) { Mutex() }.withLock {
            fresh(key) ?: load().also { values[key] = now() to it }
        }
    }

    fun clear() = values.clear()
}
