package com.hmdp.service.impl;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BeanUtil;
import com.hmdp.utils.RedisConstants;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    private final Jedis jedis = JedisConnectPool.getJedis();
    @Override
    public Shop getById(Serializable id) {
        if(jedis.exists( RedisConstants.REDIS_CATCH_SHOP_KEY + id)){
            Map<String, String> shopMap = jedis.hgetAll(RedisConstants.REDIS_CATCH_SHOP_KEY + id);
            return BeanUtil.buildShop(shopMap);
        }else{
            Shop shop = super.getById(id);
            jedis.hset(RedisConstants.REDIS_CATCH_SHOP_KEY + id, BeanUtil.buildShopMap(shop));
            jedis.expire(RedisConstants.REDIS_CATCH_SHOP_KEY + id, RedisConstants.CACHE_SHOP_TTL);
            return shop;
        }

    }
}
