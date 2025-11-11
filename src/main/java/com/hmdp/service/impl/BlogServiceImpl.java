package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private IUserService userService;

    @Override
    @Transactional
    public void likeBlog(Long id) {
        //判断当前登录的用户是否已经点赞过该条blog
        boolean idLiked = checkIsLiked(UserHolder.getUser().getId(),  id);
        //如果有，提示用户对同一blog点赞多次
        if(idLiked){
            log.warn("用户：{}。已经点赞过该条blog：{}",UserHolder.getUser().getNickName(), id);
            return;
        }
        //如果没有，则点赞,并同步到redis中
        this.update()
                .setSql("liked = liked + 1").eq("id", id).update();

        redisTemplate.opsForSet().add(RedisConstants.LIKE_BLOG_USERS + id, UserHolder.getUser().getId().toString());

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

    /**
     * 检查用户是否已经点赞过某条blog
     * @param
     * @return
     */
    private boolean checkIsLiked(Long userId,Long blogId) {

        return redisTemplate.opsForSet().isMember(RedisConstants.LIKE_BLOG_USERS + blogId, userId.toString());

    }
}
