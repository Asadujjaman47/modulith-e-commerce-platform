package com.company.ecommerce.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.ecommerce.TestcontainersConfiguration;
import com.company.ecommerce.catalog.api.dto.CategoryResponse;
import com.company.ecommerce.catalog.api.dto.CreateCategoryRequest;
import com.company.ecommerce.catalog.application.ManageCategoriesUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;

/**
 * Verifies the Redis-backed cache against a real Redis container: a read populates the cache, and a
 * write evicts it so stale data is never served.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CacheIntegrationTest {

    @Autowired private ManageCategoriesUseCase categories;
    @Autowired private CacheManager cacheManager;

    @Test
    void listIsCachedAndEvictedOnWrite() {
        Cache cache = cacheManager.getCache(CacheConfig.CATEGORY_LIST);
        assertThat(cache).isNotNull();
        cache.clear();

        // First read populates the cache.
        List<CategoryResponse> first = categories.list();
        assertThat(cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNotNull();

        int sizeBefore = first.size();

        // A write evicts the cached list (allEntries = true).
        categories.create(new CreateCategoryRequest("Cache Test " + System.nanoTime(), null, null, null));
        assertThat(cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNull();

        // Next read repopulates with fresh data reflecting the write.
        List<CategoryResponse> second = categories.list();
        assertThat(second).hasSize(sizeBefore + 1);
        assertThat(cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNotNull();
    }

    @Test
    void cacheManagerIsRedisBacked() {
        assertThat(cacheManager).isInstanceOf(
                org.springframework.data.redis.cache.RedisCacheManager.class);
    }
}
