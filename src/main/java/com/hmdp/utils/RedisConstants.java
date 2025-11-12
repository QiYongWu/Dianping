package com.hmdp.utils;

public class RedisConstants {


    //########################### config info ###################################
    public static final int REDIS_MAX_TOTAL = 200;
    public static final int REDIS_MAX_IDLE = 50;
    public static final int REDIS_MIN_IDLE = 20;
    public static final int REDIS_CONNECT_TIMEOUT = 3000;

    public static final String REDIS_HOST = "47.107.240.108";
    public static final int REDIS_PORT = 6379;

    public static final int REDIS_DATABASE = 3;

    //############################ constant keys ###################################


    public static final String CATCH_SHOP_KEY = "cache:shop:";

    public static final Long CACHE_SHOP_TTL = 600L + RandomUtil.createRandomNum() * 60;

    //商铺类型
    public static final String CATCH_SHOP_TYPE_KEY = "cache:shop:type:";

    //分布式锁
    public static final String REDIS_DISTRIBUTED_LOCK_KEY = "lock:distributed:";
    public static final long REDIS_DISTRIBUTED_LOCK_TTL = 3L;


    //异步锁
    public static final String REDIS_ASYNC_LOCK_KEY = "lock:async:";

    public static final long  REDIS_ASYNC_LOCK_TTL = 15L;

    public static final String CATCH_VOUCHER_LIST_KEY = "cache:voucher:list:";

    public static final long CACHE_VOUCHERS_LIST_TTL = 300L + RandomUtil.createRandomNum() * 60;

    public static final String PROPERTY_EXPIRE_KEY = "expire:time" ;

    public static final long LOGIN_VERIFY_CODE_TTL = 120;
    public static final String LOGIN_VERIFY_CODE = "user:login:verify:code:";

    public static final String LOGIN_TOKEN_KEY = "user:login:token:";
    public static final long LOGIN_TOKEN_TTL = 3600L;
    public static final String LIKE_BLOG_USERS = "info:like:blog";

    public static final String INFO_FOLLOW_BLOGER_KEY = "info:follow:bloger:";
}
