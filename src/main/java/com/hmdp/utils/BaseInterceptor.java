package com.hmdp.utils;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.hmdp.client.RedisClient;
import com.hmdp.entity.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

@Slf4j
@Component
public class BaseInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisClient redisClient;


    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String token = request.getHeader("Authorization");
            String key = RedisConstants.LOGIN_TOKEN_KEY + token;
            if (ObjectUtils.isEmpty(token)) {
                return true;
            }
            if (!stringRedisTemplate.hasKey(key)) {
                return true;
            }

            User user = (User) redisClient.getBean(key, User.class);
            UserHolder.saveUser(user);
            // 刷新token有效期，保持用户处于登录状态
            stringRedisTemplate.expire(key, Duration.ofSeconds(RedisConstants.LOGIN_TOKEN_TTL));
            return true;
        }catch (Exception e){
            log.error("出错:{}",e.getMessage());
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
