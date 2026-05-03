package com.stu.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 逻辑过期缓存数据包装：
 * <ul>
 *   <li>expireTime：逻辑过期时间（到点后允许返回旧值并触发异步重建）</li>
 *   <li>data：真实业务数据</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData {

    private LocalDateTime expireTime;

    private Object data;
}

