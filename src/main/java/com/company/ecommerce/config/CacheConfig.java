package com.company.ecommerce.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Spring Cache abstraction backed by Redis. Caches are read-through with per-cache TTLs; values are
 * stored as JSON (type-aware, so DTOs round-trip), keys are plain strings under a shared prefix.
 *
 * <p>Redis is a cache only — PostgreSQL is the source of truth — so cache failures must never break a
 * request. {@link #errorHandler()} downgrades any Redis error to a log + a fall-through to the
 * underlying method (a database read), keeping the application available during a Redis outage.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** Single product by id (catalog). */
    public static final String PRODUCTS = "products";

    /** Full category list (catalog). */
    public static final String CATEGORY_LIST = "categoryList";

    /** Full brand list (catalog). */
    public static final String BRAND_LIST = "brandList";

    private static final String KEY_PREFIX = "ecommerce:cache:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(DEFAULT_TTL)
                        .disableCachingNullValues()
                        .prefixCacheNameWith(KEY_PREFIX)
                        .serializeKeysWith(
                                SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(
                                SerializationPair.fromSerializer(
                                        new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache =
                Map.of(
                        PRODUCTS, base.entryTtl(Duration.ofMinutes(15)),
                        CATEGORY_LIST, base.entryTtl(Duration.ofMinutes(30)),
                        BRAND_LIST, base.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
