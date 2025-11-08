package com.hmdp.service.impl;

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
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            Map<String, String> shopMap = jedis.hgetAll(RedisConstants.REDIS_CATCH_SHOP_KEY + id);
            return BeanUtil.buildShop(shopMap);
        }else{
            Shop shop = super.getById(id);
            if(ObjectUtils.isEmpty( shop)){
                return shop;
            }else {
                jedis.hset(RedisConstants.REDIS_CATCH_SHOP_KEY + id, BeanUtil.buildShopMap(shop));
                jedis.expire(RedisConstants.REDIS_CATCH_SHOP_KEY + id, RedisConstants.CACHE_SHOP_TTL);
            }

            return shop;
        }

    }
}
