package com.hmdp.config;

import com.hmdp.utils.RedisConstants;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {


    // 处理对象的 RedisTemplate
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // 处理纯字符串的 StringRedisTemplate (Spring Boot 会自动注入，这里可省略)
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redisson 是一个在 Redis 基础上实现的 Java 驻内存数据网格(In-Memory Data Grid)。
     * 它提供了一系列的 Java 常用对象，让使用 Redis 变得更加简单。
     * @return
     */
    @Bean
    public RedissonClient redissonClient() {

        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());
        config.useSingleServer()
                .setAddress("redis://" + RedisConstants.REDIS_HOST + ":" + RedisConstants.REDIS_PORT)
                .setDatabase(RedisConstants.REDIS_DATABASE)
                .setConnectionPoolSize(RedisConstants.REDIS_MAX_TOTAL)
                .setConnectionMinimumIdleSize(RedisConstants.REDIS_MIN_IDLE);

        return Redisson.create(config);
    }

}