package com.finance.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.auth.entity.User;
import com.finance.modules.auth.mapper.UserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户信息加载
 * 实现 Spring Security 提供的 UserDetailsService 接口。
 * Spring Security 在认证时会调用 loadUserByUsername 方法，
 * 根据手机号从数据库查询用户信息，返回 UserDetails 对象
 * 登录时根据手机号 → 去数据库查用户 → 封装成 Security 认识的用户对象
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;//用来查数据库用户表

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 登录时，Spring Security 自动调用这个方法,重写Spring Security 提供的 UserDetailsService 接口的loadUserByUsername方法
     * @param phone
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        /**
         * Spring Security 在认证时自动调用此方法，根据登录名（本项目是手机号）从数据库加载用户信息，
         * 返回 UserDetails 对象。Spring Security 再自动拿这个对象和用户提交的密码做比对。
         *
         * 在 AuthServiceImpl.login() 中手动调用了 authenticationManager.authenticate(...)，
         * Security 内部就会自动找到这个 UserDetailsServiceImpl.loadUserByUsername()。
         */
        // 1. 根据手机号查数据库
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        // 2. 查不到抛异常（Spring Security 会处理）
        if (user == null) {
            throw new UsernameNotFoundException("手机号或密码错误");
        }

        // 3. 查到则组装 Spring Security 的 User 对象
        //org.springframework.security.core.userdetails.User（Spring Security 提供）,这是框架内置的唯一官方实现，我们直接 new 它使用
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),// 用户名（我们放用户ID）
                user.getPassword(),// 数据库加密密码
                user.getStatus() != null && user.getStatus() == 1 ? false : true,
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        /**
         * SimpleGrantedAuthority（实现类，Spring Security 提供）--简单权限对象
         * 包装一个权限字符串。Spring Security 用它来代表用户的"角色"或"权限"。
         *
         * 字符串必须是 ROLE_ 前缀开头，否则 .hasRole("ADMIN") 匹配不到。.hasRole("ADMIN") 内部会自动拼 ROLE_ 前缀。
         * .hasAuthority("ROLE_ADMIN") 则需要写完整的 ROLE_ADMIN。
         * getAuthority() 方法返回的是完整字符串（如 "ROLE_ADMIN"），不是 "ADMIN"。
         */
    }
}
