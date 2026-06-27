package com.company.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Treats Redis/cache failures as non-fatal: each error is logged and swallowed so the caller falls
 * through to the underlying method (a database read for {@code @Cacheable}, or a no-op for
 * put/evict). This keeps the application serving traffic if Redis is degraded or unreachable —
 * Redis is a cache, not the source of truth.
 */
@Slf4j
class LoggingCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET failed (cache={}, key={}); falling back to source", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT failed (cache={}, key={}); value not cached", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT failed (cache={}, key={})", cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR failed (cache={})", cache.getName(), exception);
    }
}
