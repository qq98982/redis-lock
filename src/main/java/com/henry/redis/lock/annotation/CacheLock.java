package com.henry.redis.lock.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CacheLock {

    // redis锁key的前缀
    String prefix() default "lock_";

    // 过期秒数,默认为5秒
    int expire() default 5;

    // 超时时间单位
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // Key的分隔符
    String delimiter() default ":";
}
