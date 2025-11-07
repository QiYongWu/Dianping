package com.hmdp.utils;

import com.hmdp.entity.User;

import java.util.HashMap;
import java.util.Map;

public class UserUtil {

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
}
