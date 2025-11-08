package com.hmdp.client;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.utils.RedisConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
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

    public static  boolean setToJSONStr(Object object, String key){
        return jedis.set(key, JSON.toJSONString(object)).equals("OK");
    }

    public static boolean setExToJSONStr(Object object, String key,Long seconds){
        return jedis.setex(key, seconds, JSON.toJSONString(object)).equals("OK");
    }

    public static boolean setExPropertyToJSONStr(Object object, String key,Long seconds){
        JSONObject jsonObject = (JSONObject) JSON.toJSON(object);
        jsonObject.put("expire", LocalTime.now().plusSeconds( seconds));
        return jedis.setex(key, seconds, JSON.toJSONString(jsonObject)).equals("OK");
    }

    public static  Object getBean(String key){
        String json = jedis.get(key);
        return JSONUtil.toBean(json, Object.class);
    }

    public static  boolean delete(String key){
        return jedis.del(key) == 1;
    }

    //互斥锁
    public boolean setToJSONStrUseLock(Object object, String key){
        if(tryLock()){
            try{
                return setToJSONStr(object, key);
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
    public boolean setToJSONStrUseLockUseLogEx(Object object, String key){
        String json = jedis.get(key);
        if(json == null){
            return false;
        }
        JSONObject jsonObject = (JSONObject) JSON.parse(json);
        LocalTime expire = LocalTime.parse(jsonObject.getString("expire"));
        if(LocalTime.now().isBefore(expire)){
            if(tryLock()){
                try{
                    return setToJSONStr(object, key);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock();
                }
            }else{
                return setToJSONStr(object, key);
            }
        }else{
            return setToJSONStr(object, key);
        }


    }


}
