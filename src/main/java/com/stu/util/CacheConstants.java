package com.stu.util;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存 Key 与 TTL 常量。
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * 缓存 Key：一级分类列表
     */
    public static final String CACHE_CATEGORY_TOP_KEY = "cache:category:top";

    /**
     * 缓存 Key 前缀：二级分类列表（按 parentId）
     */
    public static final String CACHE_CATEGORY_SUB_KEY_PREFIX = "cache:category:sub:";

    /**
     * 缓存 Key 前缀：分类详情（按 categoryId）
     */
    public static final String CACHE_CATEGORY_DETAIL_KEY_PREFIX = "cache:category:detail:";

    /**
     * 分布式锁 Key 前缀（用于缓存重建互斥）
     */
    public static final String LOCK_KEY_PREFIX = "lock:cache:";

    /**
     * 缓存空值 TTL：用于防止缓存穿透（如 categoryId 不存在）
     */
    public static final long NULL_CACHE_TTL = 2;

    public static final TimeUnit NULL_CACHE_TTL_UNIT = TimeUnit.MINUTES;

    /**
     * 分类列表缓存 TTL（加随机抖动）
     */
    public static final long CATEGORY_LIST_TTL = 30;

    public static final TimeUnit CATEGORY_LIST_TTL_UNIT = TimeUnit.MINUTES;

    /**
     * 分类详情逻辑过期时间（数据过期后可先返回旧值并异步重建）
     */
    public static final long CATEGORY_DETAIL_LOGICAL_EXPIRE = 30;

    public static final TimeUnit CATEGORY_DETAIL_LOGICAL_EXPIRE_UNIT = TimeUnit.MINUTES;

    /**
     * 分类详情 Redis 实际 TTL（物理过期时间，通常要大于逻辑过期时间）
     */
    public static final long CATEGORY_DETAIL_REDIS_TTL = 120;

    public static final TimeUnit CATEGORY_DETAIL_REDIS_TTL_UNIT = TimeUnit.MINUTES;

    /**
     * TTL 随机抖动上限（秒）：用于防止缓存雪崩（大量 Key 同时过期）
     */
    public static final int TTL_JITTER_MAX_SECONDS = 300;

    /**
     * 缓存重建分布式锁 TTL（秒）：避免死锁
     */
    public static final long LOCK_TTL_SECONDS = 10;
}

