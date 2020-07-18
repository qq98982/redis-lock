package com.henry.redis.lock.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 分布式锁key生成器
 */
public interface CacheKeyGenerator {
    // 获取AOP参数,生成指定缓存Key
    String getLockKey(ProceedingJoinPoint proceedingJoinPoint);
}
