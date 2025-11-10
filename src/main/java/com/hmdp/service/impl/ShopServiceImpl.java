package com.hmdp.service.impl;


import com.hmdp.client.RedisClient;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;


import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hmdp.utils.RedisConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;


import java.io.Serializable;

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

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisClient redisClient;


    //更改商铺信息
    @Transactional
    @Override
    public boolean updateById(Shop entity) {

        String key = RedisConstants.CATCH_SHOP_KEY + entity.getId();

        boolean save = super.updateById(entity);
        if(save) {
            redisTemplate.delete(key);
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

        String key = RedisConstants.CATCH_SHOP_KEY + id;

        if(redisTemplate.hasKey( key)){

            Object shop = redisClient.getBean(key, Shop.class);
            if(shop != null){
                return (Shop) shop;
            }else{
                return null;
            }

        }else{
            Shop shop = super.getById(id);
            redisClient.setExToJSONStr(key, RedisConstants.CACHE_SHOP_TTL, shop);
            return shop;
        }

    }

}
