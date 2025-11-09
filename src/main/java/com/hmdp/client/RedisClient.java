package com.hmdp.client;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.utils.RedisConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.time.LocalTime;

@Component
public class RedisClient {

    private static final Jedis jedis = JedisConnectPool.getJedis();

    public static boolean tryLock(){
        if(jedis.exists(RedisConstants.CATCH_LOCK_KEY) && "1".equals(jedis.get(RedisConstants.CATCH_LOCK_KEY))){
            return false;
        }else{
            return jedis.setex(RedisConstants.CATCH_LOCK_KEY, RedisConstants.CATCH_LOCK_TTL, "1").equals("OK");
        }
    }

    public static void  unlock(){
        jedis.del(RedisConstants.CATCH_LOCK_KEY);
    }

    public static  boolean setToJSONStr(String key,Object object){
        if(object == null){
            return jedis.set(key, "").equals("OK");
        }else {
            return jedis.set(key, JSON.toJSONString(object)).equals("OK");
        }
    }

    public static boolean setExToJSONStr(String key,Long seconds,Object object){
        if(object == null){
            return jedis.setex(key, seconds, "").equals("OK");
        }else{
            return jedis.setex(key, seconds, JSON.toJSONString(object)).equals("OK");
        }
    }

    //添加逻辑失效属性
    public static boolean setExPropertyToJSONStr( String key,Long seconds,Object object){
        if(object == null){
            return jedis.set(key, "").equals("OK");
        }else {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
            jsonObject.put(RedisConstants.PROPERTY_EXPIRE_KEY, LocalTime.now().plusSeconds(seconds));
            return jedis.setex(key, seconds, JSON.toJSONString(jsonObject)).equals("OK");
        }
    }

    public static Object getBean(String key,Class<?> clazz){
        String value = jedis.get(key);
        if (StringUtils.isEmpty(value)){
            return null;
        }else{
            return JSON.parseObject(value, clazz);
        }
    }

    public static JSONArray getBeanList(String key){

        String value = jedis.get(key);
        if(StringUtils.isEmpty(value)){
            return null;
        }else {
            return JSON.parseArray(jedis.get(key));
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
        String json = jedis.get(key);
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
