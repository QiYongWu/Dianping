package com.hmdp.utils;

public class RedisConstant {

    //########################### config info ###################################
    public static final int REDIS_MAX_TOTAL = 8;
    public static final int REDIS_MAX_IDLE = 8;
    public static final int REDIS_MIN_IDLE = 0;
    public static final int REDIS_CONNECT_TIMEOUT = 1000;

    public static final String REDIS_HOST = "47.107.240.108";
    public static final int REDIS_PORT = 6379;

    public static final int REDIS_DATABASE = 0;

    //############################ constant keys ###################################

    //验证码
    public static final String REDIS_KEY_USER_LOGIN_CODE = "user:login:code:";

}
