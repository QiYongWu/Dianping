package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.pool.JedisConnectPool;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.hmdp.utils.UserUtil.buildUserMap;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private  UserMapper userMapper;

    private final Jedis jedis = JedisConnectPool.getJedis();
    @Override
    public Result sendCode(String phone, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号输入不合法！");
        }
        String verifyCode = RandomUtil.createFourVerifyCode();
        session.setAttribute("code", verifyCode);

        jedis.setex(RedisConstant.REDIS_KEY_USER_LOGIN_CODE + phone + verifyCode, 120, verifyCode);

        return Result.success("验证码已发送，2分钟后过期。");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号输入不合法！");
        }


        if(!StringUtils.equals(loginForm.getCode(), (String)session.getAttribute("code"))){
            return Result.fail("验证码错误");
        }

        if(ObjectUtils.isEmpty(jedis.get(RedisConstant.REDIS_KEY_USER_LOGIN_CODE + loginForm.getPhone() + loginForm.getCode()))){
            return Result.fail("验证码已过期");
        }


        User user = userMapper.selectOne(new QueryWrapper<User>().eq("phone",loginForm.getPhone()));
        StringBuilder msg = new StringBuilder("");
        if(ObjectUtils.isEmpty( user)){
            user = new User();
            user.setPhone(loginForm.getPhone());

            user.setPassword(
                    StringUtils.isNotBlank(loginForm.getPassword()) ?
                            PasswordEncoder.encode(loginForm.getPhone()) : PasswordEncoder.encode("123456")
            );

            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
            msg.append("注册成功！");


        }else{
            msg.append("登录成功！");
        }
        session.setAttribute("user",user);

        jedis.hset(RedisConstants.LOGIN_USER_KEY,buildUserMap(user));

        return Result.ok(user);

    }

    @Override
    public Result logout(HttpSession session) {
        UserHolder.removeUser();
        session.setAttribute("user",null);
        return Result.ok();
    }


}
