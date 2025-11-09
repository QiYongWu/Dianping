package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 600L + RandomUtil.createRandomNum() * 60;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";

    public static final String LOGIN_USER_KEY = "login:user:";

    public static final Long REDIS_TOKEN_EXPIRE = 3600L;

    //########################### config info ###################################
    public static final int REDIS_MAX_TOTAL = 208;
    public static final int REDIS_MAX_IDLE = 8;
    public static final int REDIS_MIN_IDLE = 0;
    public static final int REDIS_CONNECT_TIMEOUT = 3000;

    public static final String REDIS_HOST = "47.107.240.108";
    public static final int REDIS_PORT = 6379;

    public static final int REDIS_DATABASE = 0;

    //############################ constant keys ###################################

    //验证码
    public static final String REDIS_KEY_USER_LOGIN_CODE = "user:login:code:";

    public static final String REDIS_CATCH_SHOP_KEY = "cache:shop:";

    public static final String REDIS_CATCH_SHOP_TYPE_KEY = "cache:shop:type:";

    public static final long CACHE_SHOP_TYPE_TTL = 600L + RandomUtil.createRandomNum() * 60;

    public static final String CATCH_LOCK_KEY = "catch:shop:lock";
    public static final long CATCH_LOCK_TTL = 10L;
    public static final String REDIS_CATCH_VOUCHER_LIST_KEY = "cache:voucher:list:";
    public static final long CACHE_VOUCHERS_LIST_TTL = 300L + RandomUtil.createRandomNum() * 60;
    public static final String PROPERTY_EXPIRE_KEY = "expire:time" ;
}
