package com.company.ecommerce.config.ratelimit;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Resolves a Bucket4j {@link Bucket} for a given client key + policy. Buckets are kept in Redis (via
 * a Lettuce {@link ProxyManager}) so limits are shared across application instances. If Redis cannot
 * be reached at startup the service degrades to per-instance in-memory buckets — limiting still works,
 * just not cluster-wide.
 */
@Slf4j
public class RateLimitService {

    private final ProxyManager<byte[]> proxyManager;
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(ObjectProvider<RedisClient> rateLimitRedisClient) {
        this.proxyManager = buildProxyManager(rateLimitRedisClient.getIfAvailable());
    }

    /** Whether limits are shared across instances (Redis) rather than per-instance (in-memory). */
    public boolean isDistributed() {
        return proxyManager != null;
    }

    /** Returns the bucket for {@code key}, creating it from {@code limit} on first use. */
    public Bucket resolveBucket(String key, RateLimitProperties.Limit limit) {
        if (proxyManager != null) {
            return proxyManager
                    .builder()
                    .build(key.getBytes(StandardCharsets.UTF_8), configSupplier(limit));
        }
        return localBuckets.computeIfAbsent(
                key, k -> Bucket.builder().addLimit(bandwidth(limit)).build());
    }

    private Supplier<BucketConfiguration> configSupplier(RateLimitProperties.Limit limit) {
        return () -> BucketConfiguration.builder().addLimit(bandwidth(limit)).build();
    }

    private Bandwidth bandwidth(RateLimitProperties.Limit limit) {
        return Bandwidth.builder()
                .capacity(limit.capacity())
                .refillGreedy(limit.capacity(), limit.refillPeriod())
                .build();
    }

    private ProxyManager<byte[]> buildProxyManager(RedisClient client) {
        if (client == null) {
            log.info("Rate limiting: no Redis client; using in-memory buckets");
            return null;
        }
        try {
            var connection = client.connect(ByteArrayCodec.INSTANCE);
            log.info("Rate limiting: using distributed Redis-backed buckets");
            return LettuceBasedProxyManager.builderFor(connection)
                    .withExpirationStrategy(
                            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                    Duration.ofHours(1)))
                    .build();
        } catch (RuntimeException ex) {
            log.warn(
                    "Rate limiting: Redis unavailable ({}); falling back to in-memory buckets",
                    ex.getMessage());
            return null;
        }
    }
}
