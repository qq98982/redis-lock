package com.henry.redis.lock.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ReflectionUtils;

import com.henry.redis.lock.annotation.CacheLock;
import com.henry.redis.lock.annotation.CacheParam;

/**
 * 根据注解生成分布式锁的key
 */
public class CacheKeyGeneratorImpl implements CacheKeyGenerator {
    @Override
    public String getLockKey(ProceedingJoinPoint proceedingJoinPoint) {
        final MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        final Method method = signature.getMethod();
        final CacheLock cacheLock = method.getAnnotation(CacheLock.class);
        final Object[] args = proceedingJoinPoint.getArgs();
        final Parameter[] parameters = method.getParameters();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.length; i++) {
            final CacheParam cacheParam = parameters[i].getAnnotation(CacheParam.class);
            if (cacheParam == null) {continue;}
            sb.append(cacheLock.delimiter()).append(args[i]);
        }
        // 如果上面完成后StringBuilder没有值, 说明参数上没有加CacheParam的注解
        // 现在去找实体类中的有CacheParam注解的属性
        if (sb.toString().isEmpty()) {
            // 得到所有参数的注解
            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                // 得到每个参数
                final Object obj = args[i];
                final Field[] declaredFields = obj.getClass().getDeclaredFields();
                for (Field field : declaredFields) {
                    final CacheParam annotation = field.getAnnotation(CacheParam.class);
                    if (annotation == null) {continue;}
                    field.setAccessible(Boolean.TRUE);
                    sb.append(cacheLock.delimiter()).append(ReflectionUtils.getField(field, obj));
                }
            }
        }
        return cacheLock.prefix() + sb;
    }
}
