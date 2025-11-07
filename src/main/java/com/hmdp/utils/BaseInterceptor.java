package com.hmdp.utils;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.hmdp.entity.User;
import com.hmdp.pool.JedisConnectPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class BaseInterceptor implements HandlerInterceptor {

    private final Jedis jedis = JedisConnectPool.getJedis();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String token = request.getHeader("Authorization");
            if (ObjectUtils.isEmpty(token)) {
                return true;
            }
            if (!jedis.exists(RedisConstants.LOGIN_TOKEN_KEY + token)) {
                return true;
            }
            User user = BeanUtil.buildUser(jedis.hgetAll(RedisConstants.LOGIN_TOKEN_KEY + token));
            UserHolder.saveUser(user);
            jedis.expire(RedisConstants.LOGIN_TOKEN_KEY + token, RedisConstants.REDIS_TOKEN_EXPIRE);
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
