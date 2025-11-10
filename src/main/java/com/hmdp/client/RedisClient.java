package com.hmdp.client;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.Duration;
import java.time.LocalTime;

@Component
@EnableTransactionManagement
public class RedisClient {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Transactional
    public boolean tryLock(){
        if(redisTemplate.hasKey(RedisConstants.CATCH_LOCK_KEY) && "1".equals(redisTemplate.opsForValue().get(RedisConstants.CATCH_LOCK_KEY))){
            return false;
        }else{
            redisTemplate.opsForValue().set(RedisConstants.CATCH_LOCK_KEY, "1");
            redisTemplate.expire(RedisConstants.CATCH_LOCK_KEY, Duration.ofSeconds(RedisConstants.CATCH_LOCK_TTL));
            return true;
        }
    }

    public void  unlock(){
        redisTemplate.delete(RedisConstants.CATCH_LOCK_KEY);
    }

    public boolean setToJSONStr(String key,Object object){
        if(object == null){
            redisTemplate.opsForValue().set(key, "");
            return true;
        }else {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(object));
            return true;
        }
    }

    public boolean setExToJSONStr(String key,Long seconds,Object object){
        if(object == null){
            redisTemplate.opsForValue().set(key, "");
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
            return true;
        }else{
            redisTemplate.opsForValue().set(key, JSON.toJSONString(object));
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
            return true;
        }
    }

    //添加逻辑失效属性
    public boolean setExPropertyToJSONStr( String key,Long seconds,Object object){
        if(object == null){
            redisTemplate.opsForValue().set(key, "");
            return true;
        }else {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
            jsonObject.put(RedisConstants.PROPERTY_EXPIRE_KEY, LocalTime.now().plusSeconds(seconds));
            redisTemplate.opsForValue().set(key, JSON.toJSONString(jsonObject));
            return true;
        }
    }

    public Object getBean(String key,Class<?> clazz){
        String value = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(value)){
            return null;
        }else{
            return JSON.parseObject(value, clazz);
        }
    }

    public JSONArray getBeanList(String key){

        String value = (String) redisTemplate.opsForValue().get(key);
        if(StringUtils.isEmpty(value)){
            return null;
        }else {
            return JSON.parseArray(value);
        }

    }

    //互斥锁
    public boolean setToJSONStrUseLock(String key,Object object){
        if(tryLock()){
            try{
                return setToJSONStr(key, object);
            }catch (Exception e){
                throw new RuntimeException(e);
            }finally {
                unlock();
            }
        }else{
            return false;
        }
    }

    //逻辑失效
    public boolean setToJSONStrUseLockUseLogEx(String key,Object object){
        String json = (String) redisTemplate.opsForValue().get(key);
        if(json == null){
            return false;
        }
        JSONObject jsonObject = (JSONObject) JSON.parse(json);
        LocalTime expire = LocalTime.parse(jsonObject.getString(RedisConstants.PROPERTY_EXPIRE_KEY));
        if(LocalTime.now().isBefore(expire)){
            if(tryLock()){
                try{
                    return setToJSONStr(key,object);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock();
                }
            }else{
                return setToJSONStr(key,object);
            }
        }else{
            return setToJSONStr(key, object);
        }


    }

}
