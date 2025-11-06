package com.hmdp.pool;

import com.hmdp.utils.RedisConstant;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisConnectPool {
    private static final JedisPool jedisPool;
    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //最大连接数
        jedisPoolConfig.setMaxTotal(RedisConstant.REDIS_MAX_TOTAL);
        //最大空闲链接数
        jedisPoolConfig.setMaxIdle(RedisConstant.REDIS_MAX_IDLE);
        //最小空闲链接数
        jedisPoolConfig.setMinIdle(RedisConstant.REDIS_MIN_IDLE);
        //设置等待时间
        jedisPoolConfig.setMaxWaitMillis(RedisConstant.REDIS_CONNECT_TIMEOUT);
        jedisPool = new JedisPool(jedisPoolConfig,RedisConstant.REDIS_HOST, RedisConstant.REDIS_PORT);
    }
    public static Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        jedis.select(RedisConstant.REDIS_DATABASE);
        return jedis;
    }
}
