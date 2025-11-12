package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IBlogService blogService;

    @Autowired
    private IUserService userService;

    @Override
    public Result isFollow(Long blogerId) {

        Long currentLoginUserId = UserHolder.getUser().getId();

        String key = RedisConstants.INFO_FOLLOWS_KEY + currentLoginUserId;

        if(!redisTemplate.hasKey( key) || ! redisTemplate.opsForSet().isMember(key, blogerId)){
            return Result.ok(false);
        }else{
            return Result.ok(true);
        }

    }

    @Override
    public Result follow(Long blogerId, Boolean isFollow) {

        Long currentLoginUserId = UserHolder.getUser().getId();

        String key = RedisConstants.INFO_FOLLOWS_KEY + currentLoginUserId;
        if(isFollow){
            redisTemplate.opsForSet().add(key, blogerId);
        }else{
            redisTemplate.opsForSet().remove(key, blogerId);
        }

        return Result.ok();
    }

    @Override
    public Result common(Long blogerId) {

        Long currentLoginUserId = UserHolder.getUser().getId();
        String key1 = RedisConstants.INFO_FOLLOWS_KEY + currentLoginUserId;
        String key2 = RedisConstants.INFO_FOLLOWS_KEY + blogerId;
        Set<Integer> intersection = redisTemplate.opsForSet().intersect(key1, key2);

        List< User> commonFollowUsers = new ArrayList<>();
        intersection.forEach(id -> {
            User user = userService.getById(id);
            commonFollowUsers.add(user);
        });

        return Result.ok(commonFollowUsers);
    }
}
