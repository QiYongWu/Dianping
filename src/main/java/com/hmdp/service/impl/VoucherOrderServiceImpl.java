package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;

import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
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
@EnableTransactionManagement
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private IVoucherService voucherService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker RedisIdWorker;

    @Resource
    private  VoucherOrderMapper voucherOrderMapper;

    private final Lock lock = new ReentrantLock();

    @Autowired
    private RedisIdWorker redisIdWorker;

    //秒杀优惠卷卷并生产订单
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {

        log.warn("开始秒杀优惠卷！优惠卷id:{}", voucherId);
        for (int i = 0; i < SystemConstants.MAX_TRY_LOCK_COUNT; i++) {
            if(lock.tryLock()) {
                log.warn("获取悲观锁成功！开始秒杀优惠卷！总尝试次数:{}", i);
                try {
                    return Result.ok(createVoucherOrder(voucherId));
                }catch (Exception e){
                    return Result.fail("服务器错误！" + e.getMessage());
                }finally {
                    lock.unlock();
                }
            }else{
                //休眠1s后重试
                Time.sleep(1);
            }

        }
        log.error("秒杀优惠卷失败。获取悲观锁失败!");
        return Result.fail("服务器繁忙！请稍后再试！");
    }

    private Long createVoucherOrder(Long voucherId) throws RuntimeException{

        //获取秒杀优惠卷信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //获取优惠卷信息
        Voucher voucherDetail = voucherService.getById(voucherId);

        if (voucher.getBeginTime().isAfter(LocalDateTime.now()) || voucher.getEndTime().isBefore(LocalDateTime.now())) {
            log.error("秒杀优惠卷失败。优惠卷:{{}}未开始或已结束！",voucherDetail.getSubTitle());
            throw new RuntimeException("优惠卷未开始或已结束！");
        }

        Integer stock = voucher.getStock();
        if (stock <= 0) {
            log.error("秒杀优惠卷失败。优惠卷:{{}}库存不足。当前库存数:{{}}！",voucherDetail.getSubTitle(), stock);
            throw new RuntimeException("库存不足！");
        }


        //减少库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .update();

        if (success) {
            //删除对应店铺缓存的优惠卷信息
            redisTemplate.delete(RedisConstants.CATCH_VOUCHER_LIST_KEY + voucherDetail.getShopId());

            //创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(redisIdWorker.nextId("coupon:order"));
            voucherOrder.setUserId(UserHolder.getUser().getId());
            voucherOrder.setVoucherId(voucherId);

            int insertResult = voucherOrderMapper.insert(voucherOrder);
            if (insertResult <= 0) {
                throw new RuntimeException("创建订单失败！");
            } else {
                return voucherOrder.getId();
            }

        } else {
            throw new RuntimeException("领取优惠卷失败！");
        }

    }

}
