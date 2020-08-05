package core.framework.module;

import core.framework.cache.Cache;
import core.framework.internal.cache.CacheStore;
import core.framework.internal.cache.LocalCacheStore;
import core.framework.internal.cache.RedisCacheStore;
import core.framework.internal.cache.RedisLocalCacheStore;
import core.framework.internal.cache.TestCache;
import core.framework.internal.module.ModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author neo
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {
    @Mock
    CacheStore store;
    private CacheConfig config;

    @BeforeEach
    void createCacheConfig() {
        config = new CacheConfig();
        config.initialize(new ModuleContext(null), null);
    }

    @Test
    void validate() {
        assertThatThrownBy(() -> config.validate())
                .hasMessageContaining("cache is configured but no cache added");
    }

    @Test
    void cacheStoreWithLocal() {
        config.local();

        assertThat(config.cacheStore(false)).isInstanceOf(LocalCacheStore.class);
        assertThat(config.cacheStore(true)).isInstanceOf(LocalCacheStore.class);
    }

    @Test
    void cacheStoreWithRedis() {
        config.redis("localhost");

        assertThat(config.cacheStore(false)).isInstanceOf(RedisCacheStore.class);
        assertThat(config.cacheStore(true)).isInstanceOf(RedisLocalCacheStore.class);
    }

    @Test
    void cacheName() {
        assertThat(config.cacheName(TestCache.class))
                .isEqualTo("testcache");
    }

    @Test
    void add() {
        Cache<TestCache> cache = config.add(TestCache.class, Duration.ofHours(1), store);
        assertThat(cache).isNotNull();
        assertThat(config.caches.get("testcache")).isNotNull();

        assertThatThrownBy(() -> config.add(TestCache.class, Duration.ofHours(1), store))
                .isInstanceOf(Error.class)
                .hasMessageContaining("found duplicate cache name");
    }
}
