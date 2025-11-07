package com.hmdp.utils;

import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class BeanUtil {

    public static Map<String, String> buildUserMap(User user){

        Map<String,String> userMap = new HashMap<>();
        userMap.put("id",user.getId().toString());
        userMap.put("phone",user.getPhone());
        userMap.put("nickName",user.getNickName());
        userMap.put("icon",user.getIcon());
        return userMap;

    }

    public static User buildUser(Map<String,String> userMap){
        User user = new User();
        user.setId(Long.valueOf(userMap.get("id")));
        user.setPhone(userMap.get("phone"));
        user.setNickName(userMap.get("nickName"));
        user.setIcon(userMap.get("icon"));
        return user;
    }

    public static Map<String, String> buildShopMap(Shop shop){
        Map<String,String> shopMap = new HashMap<>();
        shopMap.put("id",shop.getId().toString());
        shopMap.put("name",shop.getName());
        shopMap.put("typeId",shop.getTypeId().toString());
        shopMap.put("images",shop.getImages());
        shopMap.put("area",shop.getArea());
        shopMap.put("address",shop.getAddress());
        shopMap.put("x",shop.getX().toString());
        shopMap.put("y",shop.getY().toString());
        shopMap.put("avgPrice",shop.getAvgPrice().toString());
        shopMap.put("sold",shop.getSold().toString());
        shopMap.put("comments",shop.getComments().toString());
        shopMap.put("score",shop.getScore().toString());
        shopMap.put("openHours",shop.getOpenHours());
        shopMap.put("createTime",shop.getCreateTime().toString());
        shopMap.put("updateTime", shop.getUpdateTime().toString());
        shopMap.put("distance", ObjectUtils.isEmpty(shop.getDistance())? "" : shop.getDistance().toString());
        return shopMap;
    }

    public static Shop buildShop(Map<String, String> shopMap) {
        Shop shop = new Shop();
        shop.setId(Long.valueOf(shopMap.get("id")));
        shop.setName(shopMap.get("name"));
        shop.setTypeId(Long.valueOf(shopMap.get("typeId")));
        shop.setImages(shopMap.get("images"));
        shop.setArea(shopMap.get("area"));
        shop.setAddress(shopMap.get("address"));
        shop.setX(Double.valueOf(shopMap.get("x")));
        shop.setY(Double.valueOf(shopMap.get("y")));
        shop.setAvgPrice(Long.valueOf(Integer.valueOf(shopMap.get("avgPrice"))));
        shop.setSold(Integer.valueOf(shopMap.get("sold")));
        shop.setComments(Integer.valueOf(shopMap.get("comments")));
        shop.setScore(Integer.valueOf(shopMap.get("score")));
        shop.setOpenHours(shopMap.get("openHours"));
        shop.setCreateTime(LocalDateTime.parse(shopMap.get("createTime")));
        shop.setUpdateTime(LocalDateTime.parse(shopMap.get("updateTime")));
        shop.setDistance(StringUtils.isEmpty(shopMap.get("distance"))? null : Double.valueOf(shopMap.get("distance")));
        return shop;
    }
}
