package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;

import com.hmdp.client.RedisClient;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;

import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


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

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisClient redisClient;

    @Override
    public List<ShopType> list() {
        String key = RedisConstants.CATCH_SHOP_TYPE_KEY;

        if(redisTemplate.hasKey( key)){
            Object shopTypes = redisClient.getBeanList(key);
            return shopTypes == null ? null : (List<ShopType>) shopTypes;
        }else {
            List<ShopType> shopTypes = super.list();
            redisClient.setToJSONStrUseLock(key, shopTypes);
            return shopTypes;
        }

    }
}
