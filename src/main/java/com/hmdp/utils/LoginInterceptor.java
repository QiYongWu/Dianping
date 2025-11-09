package com.hmdp.utils;

import com.hmdp.entity.User;
import com.hmdp.pool.JedisConnectPool;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {
    private final Jedis jedis = JedisConnectPool.getJedis();
    /**
     * preHandle：在请求处理前，拦截器可以进行预处理（如权限校验），并决定是否继续处理请求。
     * postHandle：在请求处理后，视图渲染前，可以对 ModelAndView 进行修改或添加数据。
     * afterCompletion：在请求和视图渲染完成后，进行资源清理工作，如日志记录和性能监控。
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**
         * 基于Session登录
         *
            HttpSession session = request.getSession();
            Object user = session.getAttribute("user");
            if(ObjectUtils.isEmpty(user)){
                UserHolder.removeUser();
                return false;
            }else{
                UserHolder.saveUser(user);
                return true;
            }
        */

        UserHolder.removeUser();

        String token = request.getHeader("Authorization");
        if(ObjectUtils.isEmpty(token)){
            response.sendError(401,"请先登录！");
            return false;
        }

        if(!jedis.exists(RedisConstants.LOGIN_TOKEN_KEY + token)){
            response.sendError(401,"请先登录！");
            return false;
        }

        jedis.expire(RedisConstants.LOGIN_TOKEN_KEY, RedisConstants.REDIS_TOKEN_EXPIRE);
        User user = BeanUtil.buildUser(jedis.hgetAll(RedisConstants.LOGIN_TOKEN_KEY + token));
        UserHolder.saveUser(user);
        return true;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
