package com.hmdp.service.impl;

import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserInfo showUserInfo(Long userId) {
        User user = UserHolder.getUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        if (userInfo == null) {
            userInfo.setCreateTime(null);
            userInfo.setUpdateTime(null);
            return userInfo;
        }else{
            userInfo.setCreateTime(user.getCreateTime());
            userInfo.setUpdateTime(user.getUpdateTime());

            //查看粉丝数
            Set fans = redisTemplate.opsForSet().members(RedisConstants.INFO_FANS_KEY + userId);
            userInfo.setFans(fans == null ? 0 : fans.size());

            //查看关注数
            Set follows = redisTemplate.opsForSet().members(RedisConstants.INFO_FOLLOWS_KEY + userId);
            userInfo.setFollowee(follows == null ? 0 : follows.size());
            return userInfo;
        }
    }
}
