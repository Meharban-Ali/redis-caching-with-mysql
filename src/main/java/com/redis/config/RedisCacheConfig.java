package com.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {

    // ─── FIX #7: Cache names — constants rakho, typo se bachao ────────────────
    // Service mein @Cacheable(RedisCacheConfig.CACHE_PRODUCT) use karo
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCT  = "product";

    // ─── FIX #7: TTL bhi constants — ek jagah se change ho ───────────────────
    private static final Duration TTL_DEFAULT  = Duration.ofMinutes(10);
    private static final Duration TTL_PRODUCTS = Duration.ofMinutes(10);
    private static final Duration TTL_PRODUCT  = Duration.ofMinutes(30);

    // ─── Constructor Injection — field injection se behtar ────────────────────
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper redisObjectMapper;

    public RedisCacheConfig(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        this.connectionFactory  = connectionFactory;
        this.redisObjectMapper  = redisObjectMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGER
    // ═══════════════════════════════════════════════════════════════════════════

    @Bean
    @Override
    public CacheManager cacheManager() {

        RedisCacheConfiguration defaultConfig = buildDefaultCacheConfig(TTL_DEFAULT);

        // FIX #1 & #2: connectionFactory inject ki — LettuceConnectionFactory
        //              manually build karna galat tha aur compile nahi hota tha
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(buildCacheConfigurations(defaultConfig))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  KEY GENERATOR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX #6: Clean key format — "ProductService::findById::42"
     *         Pehle trailing underscore tha — "ProductService_findById_42_"
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            String paramsPart = (params == null || params.length == 0)
                    ? "no-params"
                    : Arrays.stream(params)
                            .map(p -> p != null ? p.toString() : "null")
                            .collect(Collectors.joining(","));

            return target.getClass().getSimpleName()
                    + "::" + method.getName()
                    + "::" + paramsPart;
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Base cache config — har cache isse extend karta hai */
    private RedisCacheConfiguration buildDefaultCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper)
                        )
                )
                .disableCachingNullValues();
    }

    /** Har cache ka apna TTL — ek map mein */
    private Map<String, RedisCacheConfiguration> buildCacheConfigurations(
            RedisCacheConfiguration base) {
        return Map.of(
                CACHE_PRODUCTS, base.entryTtl(TTL_PRODUCTS),
                CACHE_PRODUCT,  base.entryTtl(TTL_PRODUCT)
        );
    }
}