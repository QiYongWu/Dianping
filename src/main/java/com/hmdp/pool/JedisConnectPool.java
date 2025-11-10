package com.hmdp.pool;

import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Slf4j
public class JedisConnectPool {

    private static JedisPool jedisPool;

    static {
        try {
            initPool();
        } catch (Exception e) {
            log.error("Redis连接池初始化失败", e);
            throw new RuntimeException("Redis连接池初始化失败", e);
        }
    }

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();

        // === 基础配置 ===
        config.setMaxTotal(RedisConstants.REDIS_MAX_TOTAL);
        config.setMaxIdle(RedisConstants.REDIS_MAX_IDLE);
        config.setMinIdle(RedisConstants.REDIS_MIN_IDLE);
        config.setMaxWaitMillis(RedisConstants.REDIS_CONNECT_TIMEOUT);

        // === 连接健康检查(重要!) ===
        config.setTestOnBorrow(true);           // 借用时测试连接
        config.setTestOnReturn(false);          // 归还时不测试(性能考虑)
        config.setTestWhileIdle(true);          // 空闲时测试连接

        // === 空闲连接检测 ===
        config.setTimeBetweenEvictionRunsMillis(30000);    // 30秒检测一次
        config.setMinEvictableIdleTimeMillis(60000);       // 空闲60秒回收
        config.setNumTestsPerEvictionRun(10);              // 每次检测10个连接

        // === 阻塞行为 ===
        config.setBlockWhenExhausted(true);     // 连接耗尽时阻塞等待

        // === 创建连接池 ===
        jedisPool = new JedisPool(
                config,
                RedisConstants.REDIS_HOST,
                RedisConstants.REDIS_PORT,
                RedisConstants.REDIS_CONNECT_TIMEOUT,
                null,
                RedisConstants.REDIS_DATABASE


        );

        log.info("Redis连接池初始化成功: {}:{}",
                RedisConstants.REDIS_HOST,
                RedisConstants.REDIS_PORT);
    }

    /**
     * 获取 Jedis 连接
     * @return Jedis 实例
     */
    public static Jedis getJedis() {
        if (jedisPool == null) {
            throw new RuntimeException("Redis连接池未初始化");
        }

        try {
            // 直接返回,不需要每次 SELECT
            // 因为连接池创建时已经指定了数据库
            return jedisPool.getResource();
        } catch (Exception e) {
            log.error("获取Redis连接失败", e);
            throw new RuntimeException("获取Redis连接失败", e);
        }
    }

    /**
     * 关闭连接池(应用关闭时调用)
     */
    public static void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("Redis连接池已关闭");
        }
    }

    /**
     * 获取连接池状态信息
     */
    public static String getPoolInfo() {
        if (jedisPool == null) {
            return "连接池未初始化";
        }
        return String.format(
                "活跃连接: %d, 空闲连接: %d, 等待线程: %d",
                jedisPool.getNumActive(),
                jedisPool.getNumIdle(),
                jedisPool.getNumWaiters()
        );
    }
}