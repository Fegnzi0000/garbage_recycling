package com.stu.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存客户端封装：
 * <ul>
 *     <li>缓存穿透：缓存空值（""）+ 短 TTL</li>
 *     <li>缓存雪崩：TTL 随机抖动</li>
 *     <li>缓存击穿：逻辑过期 + 互斥锁异步重建（热点 Key）</li>
 * </ul>
 */
@Component
public class RedisCacheClient {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheClient.class);

    /**
     * 缓存重建线程池（避免阻塞请求线程）
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(4);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheClient(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(String key, Object value, long time, TimeUnit unit) {
        set(key, value, time, unit, CacheConstants.TTL_JITTER_MAX_SECONDS);
    }

    /**
     * 写入普通缓存，并在 TTL 基础上增加随机抖动（秒）。
     */
    public void set(String key, Object value, long time, TimeUnit unit, int jitterMaxSeconds) {
        if (value == null) {
            // 业务侧如需缓存 null，请调用 cacheNull
            throw new IllegalArgumentException("value must not be null, use cacheNull instead");
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            long seconds = unit.toSeconds(time);
            long finalSeconds = addJitterSeconds(seconds, jitterMaxSeconds);
            stringRedisTemplate.opsForValue().set(key, json, finalSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value for key=" + key, e);
        }
    }

    /**
     * 缓存空值（使用空字符串作为占位符），用于防止缓存穿透。
     */
    public void cacheNull(String key, long time, TimeUnit unit) {
        long seconds = unit.toSeconds(time);
        long finalSeconds = addJitterSeconds(seconds, Math.min(CacheConstants.TTL_JITTER_MAX_SECONDS, (int) seconds));
        stringRedisTemplate.opsForValue().set(key, "", finalSeconds, TimeUnit.SECONDS);
    }

    /**
     * 写入逻辑过期缓存（RedisData），并设置一个更长的物理过期时间防止脏数据长期存在。
     */
    public void setWithLogicalExpire(String key, Object value,
                                    long logicalExpireTime, TimeUnit logicalExpireUnit,
                                    long redisTtl, TimeUnit redisTtlUnit) {
        Objects.requireNonNull(value, "value must not be null");
        RedisData redisData = new RedisData(LocalDateTime.now().plusSeconds(logicalExpireUnit.toSeconds(logicalExpireTime)), value);
        try {
            String json = objectMapper.writeValueAsString(redisData);
            long seconds = redisTtlUnit.toSeconds(redisTtl);
            long finalSeconds = addJitterSeconds(seconds, CacheConstants.TTL_JITTER_MAX_SECONDS);
            stringRedisTemplate.opsForValue().set(key, json, finalSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize redisData for key=" + key, e);
        }
    }

    /**
     * 缓存穿透解决方案（cache-aside）：
     * <ul>
     *     <li>命中缓存：直接返回</li>
     *     <li>命中空值占位符：返回 null</li>
     *     <li>未命中：查库，写入缓存（或写入空值）</li>
     * </ul>
     */
    public <R> R queryWithPassThrough(String key,
                                     TypeReference<R> typeRef,
                                     Supplier<R> dbFallback,
                                     long ttl, TimeUnit ttlUnit,
                                     long nullTtl, TimeUnit nullTtlUnit) {
        String json = stringRedisTemplate.opsForValue().get(key);

        if (StringUtils.hasText(json)) {
            return readValue(json, typeRef);
        }
        if (json != null) {
            // 命中空值占位符
            return null;
        }

        R r = dbFallback.get();
        if (r == null) {
            cacheNull(key, nullTtl, nullTtlUnit);
            return null;
        }

        set(key, r, ttl, ttlUnit);
        return r;
    }

    /**
     * 缓存击穿解决方案：逻辑过期。
     * <p>
     * 数据逻辑过期后：
     * <ul>
     *   <li>先返回旧值（提升可用性）</li>
     *   <li>尝试加锁，异步重建缓存</li>
     * </ul>
     */
    public <R> R queryWithLogicalExpire(String key,
                                       TypeReference<R> typeRef,
                                       Supplier<R> dbFallback,
                                       long logicalExpireTime, TimeUnit logicalExpireUnit,
                                       long redisTtl, TimeUnit redisTtlUnit,
                                       long nullTtl, TimeUnit nullTtlUnit) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            if (json != null) {
                // 空值占位符
                return null;
            }
            // 首次查询：直接查库并写入逻辑过期缓存
            R r = dbFallback.get();
            if (r == null) {
                cacheNull(key, nullTtl, nullTtlUnit);
                return null;
            }
            setWithLogicalExpire(key, r, logicalExpireTime, logicalExpireUnit, redisTtl, redisTtlUnit);
            return r;
        }

        RedisData redisData = readRedisData(json);
        if (redisData == null || redisData.getData() == null) {
            return null;
        }

        R data = convertValue(redisData.getData(), typeRef);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime != null && expireTime.isAfter(LocalDateTime.now())) {
            // 未逻辑过期
            return data;
        }

        // 已逻辑过期：尝试异步重建
        String lockKey = CacheConstants.LOCK_KEY_PREFIX + key;
        boolean locked = tryLock(lockKey);
        if (locked) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R fresh = dbFallback.get();
                    if (fresh == null) {
                        cacheNull(key, nullTtl, nullTtlUnit);
                        return;
                    }
                    setWithLogicalExpire(key, fresh, logicalExpireTime, logicalExpireUnit, redisTtl, redisTtlUnit);
                } catch (Exception e) {
                    log.warn("Cache rebuild failed, key={}", key, e);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        // 返回旧值（即使过期）
        return data;
    }

    private RedisData readRedisData(String json) {
        try {
            // RedisData.data 为 Object，直接 readValue 会得到 LinkedHashMap，这里后续再 convert
            return objectMapper.readValue(json, RedisData.class);
        } catch (Exception e) {
            log.warn("Failed to parse RedisData, json={}", json, e);
            return null;
        }
    }

    private <R> R readValue(String json, TypeReference<R> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize json", e);
        }
    }

    private <R> R convertValue(Object value, TypeReference<R> typeRef) {
        JavaType javaType = objectMapper.getTypeFactory().constructType(typeRef.getType());
        return objectMapper.convertValue(value, javaType);
    }

    private boolean tryLock(String key) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                key,
                "1",
                CacheConstants.LOCK_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    private long addJitterSeconds(long baseSeconds, int jitterMaxSeconds) {
        if (baseSeconds <= 0) {
            return baseSeconds;
        }
        int jitter = 0;
        if (jitterMaxSeconds > 0) {
            jitter = ThreadLocalRandom.current().nextInt(jitterMaxSeconds + 1);
        }
        return baseSeconds + jitter;
    }
}


