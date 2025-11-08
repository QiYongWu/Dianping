package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
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
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final Jedis jedis = JedisConnectPool.getJedis();
    private final Lock lock = new ReentrantLock();

    @Override
    public List<ShopType> list() {

        if(jedis.exists(RedisConstants.REDIS_CATCH_SHOP_TYPE_KEY)){

            String shopTypes = jedis.get(RedisConstants.REDIS_CATCH_SHOP_TYPE_KEY);
            List<ShopType> shopTypeList = JSON.parseArray(shopTypes, ShopType.class);

            return shopTypeList;

        }else {

            List<ShopType> shopTypes = super.list();
            if(lock.tryLock()) {
                try {
                    jedis.set(RedisConstants.REDIS_CATCH_SHOP_TYPE_KEY, JSON.toJSONString(shopTypes));
                    jedis.expire(RedisConstants.REDIS_CATCH_SHOP_TYPE_KEY, RedisConstants.CACHE_SHOP_TYPE_TTL);
                }catch (Exception e){
                    log.error("出错:{}",e.getMessage());
                }finally {
                    lock.unlock();
                }
            }
            return shopTypes;
        }

    }
}
