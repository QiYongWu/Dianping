package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;

import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    void likeBlog(Long id);

    List<Blog> getHotBlogs(Integer current);

    Set<User> queryBlogLikes(Long id);

    Blog showBlogDetails(Long id);

    Result showUserBlogs(Long id);

    void saveAndPushBlog(Blog blog);

    Result queryFollowBlog();
}
