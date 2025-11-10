package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.client.RedisClient;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
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
import java.util.List;
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
    @Autowired
    private RedisClient redisClient;

    //秒杀优惠卷卷并生产订单
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {

        log.warn("开始秒杀优惠卷！优惠卷id:{}", voucherId);
        for (int i = 0; i < SystemConstants.MAX_TRY_LOCK_COUNT; i++) {
            try {
                return Result.ok(createVoucherOrder(voucherId));
            }catch (Exception e){
                if(e.getMessage().contains("扣减库存失败")) {
                    log.error("获取乐观锁失败，开始第{}次尝试", i + 1);
                }else{
                    return Result.fail(e.getMessage());
                }
            }
            Time.sleep(1);
        }

        return Result.fail("服务器繁忙！请稍后再试！");
    }

    private Long createVoucherOrder(Long voucherId) throws RuntimeException{
        boolean checkFlag = false;
        User user = UserHolder.getUser();
        String prefix = "create_voucher_order:" + voucherId  + "_user:" + user.getId();
        for (int i = 0; i < SystemConstants.MAX_TRY_LOCK_COUNT; i++) {
            if(redisClient.tryLock(prefix)) {
                try {
                    Integer count = voucherOrderMapper.selectCount
                            (new QueryWrapper<VoucherOrder>()
                                    .eq("user_id", user.getId())
                                    .eq("voucher_id", voucherId));
                    if (count > 0) {
                        log.error("用户:{{}}已购买过此优惠卷！", user.getId());
                        return null;
                    }
                    checkFlag = true;
                    break;
                }catch (Exception e){
                    log.error("出错！" + e.getMessage());
                }finally {
                    redisClient.unlock(prefix);
                }
            }else{
                Time.sleep(1);
            }
        }

        if(!checkFlag){
            throw new RuntimeException("判断用户是否已经获取过优惠卷失败！");
        }

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
                .eq("stock", stock)
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
            throw new RuntimeException("扣减库存失败！");
        }

    }

}
