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

    /**
     * 全局唯一性：
     * static 确保了 JVM_ID 在类加载时只会生成一次，并且在整个 JVM 中是唯一的，不会随着对象实例化而重复生成。
     *
     * 线程共享：
     * 所有线程都能访问同一个 JVM_ID，保证了线程之间对这个标识的共享。
     *
     * 效率：
     * 如果不加 static，每次实例化对象时都会生成一个新的 UUID，这样就失去了原本的唯一性和全局共享的优势。
     */

    /**
     * 类级别的变量和方法	static 成员是 类级别的，所有对象共享这一个变量或方法。
     * 不依赖对象实例	静态方法可以直接通过类名调用，不需要实例化类。
     * 内存管理	静态成员存储在 方法区，只存在一个副本，多个实例共享。
     * 访问限制	静态方法无法直接访问实例变量和实例方法，但可以访问其他静态变量和方法。
     * 静态代码块	在类加载时执行一次，通常用于类初始化。
     * 静态内部类	静态内部类是类级别的，不需要外部类的实例即可使用。
     */
    //区分不同的JVM，避免不同的JVM间造成的线程冲突
    private static final String JVM_ID = UUID.randomUUID().toString();


    public ILockImpl(StringRedisTemplate stringRedisTemplate, String lastFix) {
        this.lastFix = lastFix;
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockKey = JVM_ID + RedisConstants.REDIS_DISTRIBUTED_LOCK_KEY + lastFix;
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
