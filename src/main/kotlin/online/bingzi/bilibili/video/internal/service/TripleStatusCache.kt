package online.bingzi.bilibili.video.internal.service

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object TripleStatusCache {

    private const val SUCCESS_TTL_NANOS = 30_000_000_000L
    private const val FAILURE_TTL_NANOS = 30_000_000_000L

    private data class Key(
        val playerId: UUID,
        val bvid: String
    )

    private data class Entry(
        val expiresAt: Long,
        val result: CredentialService.TripleCheckResult
    )

    private val cache = ConcurrentHashMap<Key, Entry>()

    private val loading = ConcurrentHashMap<
            Key,
            CompletableFuture<CredentialService.TripleCheckResult>
            >()

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "bilibili-triple-check").apply {
            isDaemon = true
        }
    }

    fun getOrLoad(
        playerId: UUID,
        bvid: String
    ): CompletableFuture<CredentialService.TripleCheckResult> {
        val key = Key(playerId, bvid)
        val now = System.nanoTime()

        cache[key]?.let { entry ->
            if (entry.expiresAt > now) {
                return CompletableFuture.completedFuture(entry.result)
            }

            cache.remove(key, entry)
        }

        return loading.computeIfAbsent(key) {
            val future = CompletableFuture.supplyAsync(
                {
                    CredentialService.checkTripleByPlayer(playerId, bvid)
                },
                executor
            )

            future.whenComplete { result, error ->
                loading.remove(key, future)

                if (error == null && result != null) {
                    val ttl = if (result.success) {
                        SUCCESS_TTL_NANOS
                    } else {
                        FAILURE_TTL_NANOS
                    }

                    cache[key] = Entry(
                        expiresAt = System.nanoTime() + ttl,
                        result = result
                    )
                }
            }

            future
        }
    }

    fun invalidate(playerId: UUID, bvid: String) {
        cache.remove(Key(playerId, bvid))
    }

    fun clearPlayer(playerId: UUID) {
        cache.keys.removeIf { it.playerId == playerId }
    }

    fun close() {
        executor.shutdownNow()
        cache.clear()
        loading.clear()
    }
}