package com.hmdp.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 * 复用一组固定线程去执行大量短期任务，避免频繁创建和销毁线程。
 * corePoolSize: 线程池中核心线程的数量，默认情况下核心线程会一直存活，即使处于闲置状态。
 * maximumPoolSize: 线程池中最大线程数量，默认情况下线程池会根据需要创建新线程，直到达到最大线程数量。
 * keepAliveTime: 非核心线程闲置时间，超过这个时间就会被回收。
 * workQueue: 线程池中的任务队列，用于存放待处理的任务。
 * threadFactory: 创建线程的工厂，用于创建新的线程。
 */
public class ThreadPool {

    private static final ExecutorService CACHE_DELETE_POOL;
    static {
        CACHE_DELETE_POOL = new ThreadPoolExecutor(
                3,
                6,
                1000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }


    public static ExecutorService getCacheDeletePool() {
        return CACHE_DELETE_POOL;
    }


}
