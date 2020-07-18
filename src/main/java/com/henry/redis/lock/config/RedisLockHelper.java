package com.henry.redis.lock.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;

/**
 * Redis分布式锁的工具
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@AllArgsConstructor
public class RedisLockHelper {
    private static final String DELIMITER = "|";
    private Executor customServiceExecutor;
    private Executor asyncServiceExecutor;
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取锁
     *
     * 首先尝试去设置锁, 如果拿到, 设置一个过期时间, 返回true
     * 如果没有拿到, 计算原先的锁是不是已经到期了, 如果到期了, 设置好一个新的过期时间+自己的一个客户端uuid, 再拿到锁
     *
     * @param lockKey lockKey
     * @param uuid    UUID
     * @param timeout 超时时间
     * @param unit    过期单位
     * @return true or false
     */
    public Boolean lock(String lockKey, final String uuid, long timeout, final TimeUnit unit) {
        final long milliseconds = Expiration.from(timeout, unit).getExpirationTimeInMilliseconds();
        // 尝试去设置锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,
                                                                        (System.currentTimeMillis()
                                                                         + milliseconds) + DELIMITER
                                                                        + uuid);
        // 如果设置好了锁,加上过期时间
        if (success != null && success) {
            stringRedisTemplate.expire(lockKey, timeout, TimeUnit.SECONDS);
        } else {

            final String oldVal = stringRedisTemplate.opsForValue().get(lockKey);
            boolean resetLock = false;
            // 如果在拿以前value的过程中没有拿到值(访问redis时延迟大, 没拿到数据, 过期了), 或者拿到了, 计算出来时间已经过期了
            // 这两种情况下,都要重新加上一个新锁, 将锁的value设置为当前时间+过期时间+uuid
            if (StringUtils.isEmpty(oldVal)) {
                resetLock = true;
            } else {
                final String[] oldValues = oldVal.split(Pattern.quote(DELIMITER));
                if (Long.parseLong(oldValues[0]) + 1 <= System.currentTimeMillis()) {
                    resetLock = true;
                }
            }
            if (resetLock) {
                success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, (System.currentTimeMillis()
                                                                                  + milliseconds) + DELIMITER
                                                                                 + uuid);
                if (success != null && success) {
                    stringRedisTemplate.expire(lockKey, timeout, TimeUnit.SECONDS);
                }
            }
        }
        return success == null ? false : success;
    }

    /**
     * 立即解锁
     */
    public void unlock(String lockKey, String value) {
        unlock(lockKey, value, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 延迟unlock
     *
     * @param lockKey   key
     * @param uuid      client(最好是唯一键的)
     * @param delayTime 延迟时间
     * @param unit      时间单位
     */
    public void unlock(final String lockKey, final String uuid, long delayTime, TimeUnit unit) {
        if (StringUtils.isEmpty(lockKey)) {
            return;
        }
        if (delayTime <= 0) {
            doUnlock(lockKey, uuid);
        } else {
            ((ScheduledExecutorService) asyncServiceExecutor).schedule(() -> doUnlock(lockKey, uuid), delayTime,
                                                                       unit);
        }
    }

    /**
     * 解锁
     *
     * 自己拿到了key返回的数据, 验证是不是自己的锁, 如果是自己的,可以删除这个key
     * 如果不是自己的uuid, 也不能删除(意味着不会有同样key的新锁生成)
     * @param lockKey key
     * @param uuid    client(最好是唯一键的)
     */
    private void doUnlock(final String lockKey, final String uuid) {
        final String val = stringRedisTemplate.opsForValue().get(lockKey);
        if (StringUtils.isEmpty(val)) {return;}
        final String[] values = val.split(Pattern.quote(DELIMITER));
        if (values.length <= 0) {
            return;
        }
        if (uuid.equals(values[1])) {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
