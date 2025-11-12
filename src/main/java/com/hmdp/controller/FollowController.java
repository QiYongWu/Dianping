package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /**
     * 返回当前登录用户是否已经关注该博主
     * @param blogerId 博主的id
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long blogerId) {
        return followService.isFollow(blogerId);
    }

    /**
     * 关注或取消关注
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long blogerId, @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(blogerId, isFollow);
    }

    /**
     * 查看共同关注
     */
    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long blogerId) {
        return followService.common(blogerId);
    }

}
