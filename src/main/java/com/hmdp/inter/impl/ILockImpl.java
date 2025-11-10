package com.hmdp.inter.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.client.RedisClient;
import com.hmdp.inter.ILock;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ILockImpl implements ILock {

    //基于Redis的分布式锁的key值
    private String lockKey;

    //区分不同业务
    private String lastFix;
    private StringRedisTemplate stringRedisTemplate;


    public ILockImpl(StringRedisTemplate stringRedisTemplate, String lastFix) {
        this.lastFix = lastFix;
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockKey = RedisConstants.REDIS_DISTRIBUTED_LOCK_KEY + lastFix;
    }

    @Override
    public boolean tryLock() {
        Long currentTheadId =  Thread.currentThread().getId();
        log.warn("线程：[{}]尝试获取分布式锁", currentTheadId);
        if(stringRedisTemplate.hasKey(lockKey)){
            log.warn("线程：[{}]获取分布式锁失败", currentTheadId);
            return false;

        }else{
            //只有当 key 不存在 时才会设置成功，保证原子性
            boolean opResult = stringRedisTemplate.opsForValue().setIfAbsent(
                                                    lockKey,
                                                    currentTheadId.toString(),
                                                    RedisConstants.REDIS_DISTRIBUTED_LOCK_TTL,
                                                    TimeUnit.SECONDS);

            log.warn("线程：[{}]获取分布式锁结果:[{}]", currentTheadId, opResult);

            return opResult;

        }
    }

    @Override
    public boolean unlock() {

        Long currentTheadId =  Thread.currentThread().getId();
        if(!stringRedisTemplate.hasKey(lockKey)){
            log.warn("线程：[{}]释放分布式锁成功", currentTheadId);
            return true;
        }
        String savedThreadId = stringRedisTemplate.opsForValue().get(lockKey);
        if(!StringUtils.equals(savedThreadId,currentTheadId.toString())){
            return false;
        }
        boolean opResult = stringRedisTemplate.delete(lockKey);
        log.warn("线程：[{}]释放分布式锁结果:[{}]", currentTheadId, opResult);
        return opResult;
    }

}
