package com.hmdp.utils;

import java.util.UUID;

public class RandomUtil {

    /**
     * 生成4位验证码
     * @return
     */
    public static String createFourVerifyCode(){
        return ((Integer)((int)((Math.random()*9+1)*1000))).toString();
    }

    public static String createUUID() {
        return ((UUID) UUID.randomUUID()).toString();
    }

    /**
     * 生产1-5的随机数
     */
    public static int createRandomNum(){
        return (int)(Math.random()*5+1);
    }
}
