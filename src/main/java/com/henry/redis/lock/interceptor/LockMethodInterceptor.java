package com.henry.redis.lock.interceptor;

import java.lang.reflect.Method;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.henry.redis.lock.annotation.CacheLock;
import com.henry.redis.lock.config.RedisLockHelper;

import lombok.AllArgsConstructor;

/**
 * 分布式锁的拦截器
 */
@Configuration
@Aspect
@AllArgsConstructor
public class LockMethodInterceptor {
    private final RedisLockHelper redisLockHelper;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Around("execution(public * *(..)) && @annotation(com.henry.redis.lock.annotation.CacheLock)")
    public Object interceptor(ProceedingJoinPoint pjp) {
        final MethodSignature signature = (MethodSignature) pjp.getSignature();
        final Method method = signature.getMethod();
        final CacheLock lock = method.getAnnotation(CacheLock.class);
        if (StringUtils.isEmpty(lock.prefix())) {
            throw new RuntimeException("lock key don't null...");
        }
        final String lockKey = cacheKeyGenerator.getLockKey(pjp);
        final String value = UUID.randomUUID().toString();
        try {
            // 假设上锁成功，但是设置过期时间失效，以后拿到的都是 false
            final boolean success = redisLockHelper.lock(lockKey, value, lock.expire(), lock.timeUnit());
            if (!success) {
                throw new RuntimeException("重复提交");
            }
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException("系统异常");
            }
        } finally {
            // TODO 如果演示的话需要注释该代码;实际应该放开
//            redisLockHelper.unlock(lockKey, value);
        }
    }
}

