package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.client.RedisClient;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;

import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

// @EnableTransactionManagement: 开启事务功能（总开关）
// @Transactional: 标记哪些方法需要事务（具体开关）

@Service
@EnableTransactionManagement
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private  UserMapper userMapper;

    @Autowired
    private RedisClient redisClient;

    @Override
    @Transactional
    public Result sendCode(String phone, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号输入不合法！");
        }

        String verifyCode = RandomUtil.createFourVerifyCode();


        String key = RedisConstants.LOGIN_VERIFY_CODE + phone + verifyCode;

        stringRedisTemplate.opsForValue().set(key, verifyCode);
        stringRedisTemplate.expire(key, Duration.ofSeconds(RedisConstants.LOGIN_VERIFY_CODE_TTL));


        return Result.success("验证码已发送，2分钟后过期。");
    }


    @Transactional
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号输入不合法！");
        }

        String key = RedisConstants.LOGIN_VERIFY_CODE + loginForm.getPhone() + loginForm.getCode();

        String verifyCode = stringRedisTemplate.opsForValue().get(key);

        if(StringUtils.isBlank(verifyCode)){
            return Result.fail("验证码已过期或输入的验证码不正确");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("phone",loginForm.getPhone()));

        StringBuilder msg = new StringBuilder("");

        if(ObjectUtils.isEmpty( user)){
            user = new User();
            user.setPhone(loginForm.getPhone());

            user.setPassword(StringUtils.isNotBlank(loginForm.getPassword()) ?
                            PasswordEncoder.encode(loginForm.getPhone()) : PasswordEncoder.encode("123456"));
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            msg.append("注册成功！");

        }else{
            msg.append("登录成功！");
        }

        UserHolder.saveUser(user);

        String token  = RandomUtil.createUUID();

        redisClient.setExToJSONStr(RedisConstants.LOGIN_TOKEN_KEY + token , RedisConstants.LOGIN_TOKEN_TTL , user);

        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        UserHolder.removeUser();
        stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
        return Result.ok();
    }


}
