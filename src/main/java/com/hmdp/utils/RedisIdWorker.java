package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1609459200L; // 2021-01-01 00:00:00
    @Autowired
    private RedisTemplate redisTemplate;

    public Long nextId(String keyPrefix){

        long currentEpochSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
        long epochSecond = currentEpochSecond - BEGIN_TIMESTAMP;

        String key = "id:" + keyPrefix + ":" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        //redis 自增功能，若key值不存在，自动创建key并赋值为1
        Long count = redisTemplate.opsForValue().increment(key, 1);

        redisTemplate.expire(key, 1, TimeUnit.DAYS);

        return (epochSecond << 32) | count;

    }

}
