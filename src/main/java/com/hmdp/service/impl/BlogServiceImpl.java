package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IUserInfoService userInfoMapper;

    @Autowired
    private IUserService userService;

    @Override
    @Transactional
    public void likeBlog(Long id) {
        //判断当前登录的用户是否已经点赞过该条blog
        boolean idLiked = checkIsLiked(UserHolder.getUser().getId(), id);
        //如果有，提示用户对同一blog点赞多次
        if(idLiked){
            log.warn("用户：{}。已经点赞过该条blog：{}",UserHolder.getUser().getNickName(), id);
            return;
        }
        //如果没有，则点赞,并同步到redis中
        this.update()
                .setSql("liked = liked + 1").eq("id", id).update();
        redisTemplate.opsForZSet().add(RedisConstants.LIKE_BLOG_USERS + id, UserHolder.getUser().getId().toString(), System.currentTimeMillis());

    }

    @Override
    public List<Blog> getHotBlogs(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIsLike(checkIsLiked(UserHolder.getUser().getId(), blog.getId()));
            blog.setIcon(user.getIcon());
        });

        return records;
    }

    // 查询blog的点赞用户，返回最先点赞的5个人的列表
    @Override
    public Set<User> queryBlogLikes(Long id) {
        //0->4 分数从低到高，分数越低，点赞时间越早
        Set<String> userIds = redisTemplate.opsForZSet().range(RedisConstants.LIKE_BLOG_USERS + id, 0, 4);
        Set<User> users = new HashSet<>();
        if(!ObjectUtils.isEmpty(userIds)){
            users = userIds.stream().map(userId -> {
                User user = userService.getById(Long.parseLong(userId));
                user.setIcon(user.getIcon());
                return user;
            }).collect(Collectors.toSet());
        }
        return users;
    }

    @Override
    public Blog showBlogDetails(Long id) {
        Blog blog = this.getById(id);
        if(blog == null){
            return null;
        }
        User user = userService.getById(blog.getUserId());
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        blog.setIsLike(checkIsLiked(UserHolder.getUser().getId(), blog.getId()));
        return blog;
    }

    @Override
    public Result showUserBlogs(Long id) {
        return null;
    }

    /**
     * 检查用户是否已经点赞过某条blog
     * @param
     * @return
     */
    private boolean checkIsLiked(Long userId,Long blogId) {
        return !ObjectUtils.isEmpty(redisTemplate.opsForZSet().score(RedisConstants.LIKE_BLOG_USERS + blogId, userId.toString()));
    }
}
