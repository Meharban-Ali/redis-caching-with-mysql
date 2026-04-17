// ─────────────────────────────────────────────────────────────
// RedisUtil.java  — FIX #9: Redis direct operations alag class
// ─────────────────────────────────────────────────────────────
package com.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // Direct Redis se value fetch karo
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.info("Redis fetch — key: {}, found: {}", key, value != null);
        return value;
    }

    // Direct Redis mein value store karo with TTL
    public void set(String key, Object value, long ttlMinutes) {
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
        log.info("Redis store — key: {}, TTL: {} min", key, ttlMinutes);
    }

    // Key exist karta hai ya nahi
    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}