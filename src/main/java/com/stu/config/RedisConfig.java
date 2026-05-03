package com.stu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 基础配置。
 * <p>
 * 本项目优先使用 {@link StringRedisTemplate}（key/value 均为 String），
 * 由业务侧自行负责 JSON 序列化，以便实现：缓存空值、随机 TTL、逻辑过期等高级策略。
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}

