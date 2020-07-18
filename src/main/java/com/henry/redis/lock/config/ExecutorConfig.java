package com.henry.redis.lock.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import cn.hutool.core.thread.ThreadFactoryBuilder;

/**
 * 两种不同的多线程池
 */
@Configuration
@EnableAsync
@Lazy
@ConditionalOnMissingBean(Executor.class)
public class ExecutorConfig {

    @Value("${custom.executor.thread.core_pool_size}")
    private int customCorePoolSize;
    @Value("${custom.executor.thread.max_pool_size}")
    private int customMaxPoolSize;
    @Value("${custom.executor.thread.keep_alive_time}")
    private int customKeepAliveTime;
    @Value("${custom.executor.thread.blocking_queue_capacity}")
    private int customQueueCapacity;
    @Value("${custom.executor.thread.name.prefix}")
    private String customPrefix;

    /**
     * 任务不能提交时,抛出异常,反馈程序运行状态, 关键业务通常使用此拒绝策略, 如果异常,可以提早发现
     */
    @Bean(name = "customServiceExecutor")
    public Executor customServiceExecutor() {
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(customCorePoolSize, customMaxPoolSize, customKeepAliveTime,
                                       TimeUnit.SECONDS,
                                       new ArrayBlockingQueue<>(customQueueCapacity),
                                       new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadFactory(new ThreadFactoryBuilder().setNamePrefix(customPrefix)
                                                            .setDaemon(true).build());
        return executor;
    }

    @Value("${async.executor.thread.core_pool_size}")
    private int asyncCorePoolSize;
    @Value("${async.executor.thread.max_pool_size}")
    private int asyncMaxPoolSize;
    @Value("${async.executor.thread.blocking_queue_capacity}")
    private int asyncQueueCapacity;
    @Value("${async.executor.thread.name.prefix}")
    private String asyncPrefix;

    /**
     * 由提交任务的线程处理, 需要让所有任务完成, 这个是增大吞吐量的手段, 最终每个任务都执行完毕
     */
    @Bean(name = "asyncServiceExecutor")
    public Executor asyncServiceExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(asyncCorePoolSize);
        //配置最大线程数
        executor.setMaxPoolSize(asyncMaxPoolSize);
        //配置队列大小
        executor.setQueueCapacity(asyncQueueCapacity);
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix(asyncPrefix);

        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }
}