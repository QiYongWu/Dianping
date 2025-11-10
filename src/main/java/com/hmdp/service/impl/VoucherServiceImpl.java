package com.hmdp.service.impl;


import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.client.RedisClient;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;

import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisClient redisClient;

    /**
     * 获取店铺所有的优惠卷
     * @param shopId
     * @return
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        String key = RedisConstants.CATCH_VOUCHER_LIST_KEY + shopId;
        Object voucherList =  redisClient.getBeanList(key);

        if(StringUtils.isEmpty(voucherList)){
            // 查询优惠券信息
            List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
            //缓存商铺的优惠卷信息
            redisClient.setExToJSONStr(key, RedisConstants.CACHE_VOUCHERS_LIST_TTL,vouchers);
            // 返回结果
            return Result.ok(vouchers);

        }else{
            return Result.ok(voucherList);
        }

    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }
}
