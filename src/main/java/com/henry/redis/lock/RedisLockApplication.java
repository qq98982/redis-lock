package com.henry.redis.lock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.henry.redis.lock.interceptor.CacheKeyGenerator;
import com.henry.redis.lock.interceptor.CacheKeyGeneratorImpl;

/**
 * 使用Redis分布式锁, 解决重复提交的问题
 */
@SpringBootApplication
public class RedisLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLockApplication.class, args);
    }

    @Bean(name = "cacheKeyGenerator")
    public CacheKeyGenerator cacheKeyGenerator() {
        return new CacheKeyGeneratorImpl();
    }
}
