package com.hmdp.service.impl;

import cn.hutool.json.JSONString;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.pool.ThreadPool;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BeanUtil;
import com.hmdp.utils.RedisConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@EnableTransactionManagement
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final ExecutorService executorService = ThreadPool.getCacheDeletePool();

    private final Jedis jedis = JedisConnectPool.getJedis();

    private final Lock lock = new ReentrantLock();



    @Transactional
    @Override
    public boolean updateById(Shop entity) {

        boolean save = super.updateById(entity);
        if(save) {
            executorService.submit(
                    () -> {
                        if (jedis.exists(RedisConstants.REDIS_CATCH_SHOP_KEY + entity.getId())) {
                            jedis.del(RedisConstants.REDIS_CATCH_SHOP_KEY + entity.getId());
                        }
                    });
        }
        return save;
    }


    /**
     * 重写getById方法
     * @description 先查询缓存，若未命中，再查询数据库，并将数据写入缓存
     * @param id
     * @return
     */
    @Override
    public Shop getById(Serializable id) {

        if(jedis.exists( RedisConstants.REDIS_CATCH_SHOP_KEY + id)){

            String shopStr = jedis.get(RedisConstants.REDIS_CATCH_SHOP_KEY + id);

            if(StringUtils.isEmpty(shopStr)){
                return null;
            }

            JSONObject shopJSONObject = (JSONObject) JSON.parse(shopStr);
            LocalTime expire =LocalTime.parse(shopJSONObject.getString("expire"));


            if(expire.isBefore(LocalTime.now())){
                if(lock.tryLock()) {
                    try {
                        Shop shop = super.getById(id);
                        JSONObject newShopJSONObject = (JSONObject) JSON.toJSON( shop);
                        newShopJSONObject.put("expire", LocalTime.now().plusSeconds( RedisConstants.CACHE_SHOP_TTL));
                        jedis.set(RedisConstants.REDIS_CATCH_SHOP_KEY + id, JSON.toJSONString(newShopJSONObject));
                        return shop;
                    }catch (Exception e){
                        throw new RuntimeException( e);
                    }finally {
                        lock.unlock();
                    }
                }else{
                    return JSONUtil.toBean(shopStr, Shop.class);
                }
            }else{
                return JSONUtil.toBean(shopStr, Shop.class);
            }


        }else{
            Shop shop = super.getById(id);
            if (shop == null) {
                jedis.set(RedisConstants.REDIS_CATCH_SHOP_KEY + id, "");
            } else {
                JSONObject shopJSONObject = (JSONObject) JSON.toJSON( shop);
                shopJSONObject.put("expire", LocalTime.now().plusSeconds( RedisConstants.CACHE_SHOP_TTL));
                jedis.set(RedisConstants.REDIS_CATCH_SHOP_KEY + id, JSON.toJSONString(shopJSONObject));
            }
            return shop;
        }



    }

}
