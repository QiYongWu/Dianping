package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.client.RedisClient;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.inter.impl.ILockImpl;
import com.hmdp.mapper.VoucherOrderMapper;

import com.hmdp.pool.ThreadPool;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Time;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private  VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private LockUtil lockUtil;

    @Qualifier("stringRedisTemplate")
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final ExecutorService executorService = ThreadPool.getCacheDeletePool();

    private static final String JVM_ID = UUID.randomUUID().toString();

    //秒杀优惠卷并生成订单
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) throws InterruptedException,RuntimeException {

        //获取秒杀优惠卷信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        //获取优惠卷详细信息
        Voucher voucherDetail = voucherService.getById(voucherId);

        if(!checkVoucherEffective(voucher,voucherDetail)){
            return Result.fail("优惠卷不可用！活动未开始或库存不足");
        }
        log.warn("开始秒杀优惠卷！优惠卷id:{}", voucherId);

        //一人一单校验
        if(!checkUserGetVoucher(voucherId)){
            return Result.fail("服务器繁忙或您已领取过此优惠卷");
        }


        //减少库存
        if(!reduceStock(voucherId,voucher.getStock())){
            //减少库存失败，回滚事务
            throw new RuntimeException("服务器繁忙！");
        }

        //创建订单
        Long orderId = createVoucherOrder(voucherDetail,voucherId);
        if(orderId == null){
            return Result.fail("创建订单失败，请联系管理员！");
        }else{
            return Result.ok(orderId);
        }

    }


    /**
     * 判断用户是否已经获得过此优惠卷
     * @param voucherId
     * @return
     */
    private boolean checkUserGetVoucher(Long voucherId) throws InterruptedException{
        User user = UserHolder.getUser();

        Long userId = user.getId();

        String lastFix =  voucherId.toString() + userId;

        //构造【优惠卷id+用户id】的分布式锁，防止线程并发执行：判断用户是否购买过此优惠卷 发生异常

        RLock lock = redissonClient.getLock(JVM_ID + RedisConstants.REDIS_DISTRIBUTED_LOCK_KEY + lastFix);

        boolean tryLockResult = lock.tryLock(2, 1, TimeUnit.SECONDS);

        if(!tryLockResult){
            log.error("用户:{}。获取分布式锁失败！无法判断用户是否已经获得过此优惠卷",userId);
            return false;
        }

        Integer count = voucherOrderMapper.selectCount
                (new QueryWrapper<VoucherOrder>()
                        .eq("user_id", user.getId())
                        .eq("voucher_id", voucherId));
        if (count > 0) {
            log.error("用户:{{}}已经获得过此优惠卷！", user.getId());
            return false;
        }

        return true;
    }

    /**
     * 减少库存
     * @param voucherId
     * @return
     */

    private boolean reduceStock(Long voucherId,Integer stock){
        //基于乐观锁减少库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .eq("stock", stock)
                .update();
        return success;
    }

    /**
     * 创建订单
     * @param voucherId
     * @return
     */
    private Long createVoucherOrder( Voucher voucherDetail,Long voucherId){

        //删除对应店铺缓存的优惠卷信息
        redisTemplate.delete(RedisConstants.CATCH_VOUCHER_LIST_KEY + voucherDetail.getShopId());

        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("coupon:order"));

        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);

        int insertResult = voucherOrderMapper.insert(voucherOrder);
        if (insertResult <= 0) {
            return null;
        } else {
            return voucherOrder.getId();
        }

    }


    /**
     * 判断优惠卷是否有效
     */
    private boolean checkVoucherEffective (SeckillVoucher voucher,Voucher voucherDetail) {

        if (voucher.getBeginTime().isAfter(LocalDateTime.now()) || voucher.getEndTime().isBefore(LocalDateTime.now())) {
            log.error("秒杀优惠卷失败。优惠卷:{{}}未开始或已结束！", voucherDetail.getSubTitle());
            return false;
        }

        Integer stock = voucher.getStock();
        if (stock <= 0) {
            log.error("秒杀优惠卷失败。优惠卷:{{}}库存不足。当前库存数:{{}}！", voucherDetail.getSubTitle(), stock);
            return false;
        }
        return true;
    }


}
