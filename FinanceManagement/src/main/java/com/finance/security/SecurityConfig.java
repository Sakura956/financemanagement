package com.finance.security;

import com.finance.util.RedisUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 * 哪些接口不需要认证
 * 哪些接口需要管理员角色（
 * 注册 JwtAuthenticationFilter 到过滤器链
 * 配置密码加密器
 */
@Configuration
@EnableWebSecurity// 开启 Spring Security 的 Web 安全功能。它会自动导入 Spring Security 的默认配置。
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;//JWT 工具
    private final RedisUtil redisUtil;//Redis 工具
    private final CustomAccessDeniedHandler accessDeniedHandler;//权限不足处理器

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, RedisUtil redisUtil,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisUtil = redisUtil;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * SecurityFilterChain —— 安全过滤器链
     * 核心配置方法:配置安全规则
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())// CSRF 必须关闭！CSRF 防护依赖 Session，无状态模式下不适用。
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))//设置无状态（不使用 session）
                // URL 权限规则
            .authorizeHttpRequests(auth -> auth
                // 认证接口 - 无需认证
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                // 管理端接口 - 需要 ADMIN 角色
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 用户端接口 - 需要认证
                .requestMatchers("/api/v1/user/**").authenticated()
                // 其他请求允许
                .anyRequest().permitAll()
            )
                //403 自定义处理器
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))//权限不足时使用自定义处理器
                //把 JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, redisUtil),
                    UsernamePasswordAuthenticationFilter.class);//加入 JWT 过滤器

        //构建并返回配置
        return http.build();
    }

    /**
     * 密码加密器
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     * Spring Security 进行登录验证
     * @param config
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
        /**
         * 内部工作流程：
         * 接收一个未认证的 Authentication(认证对象)（本项目是 UsernamePasswordAuthenticationToken）
         * 迭代所有注册的 AuthenticationProvider
         * 找到能处理该类型的 Provider（本项目用的是 DaoAuthenticationProvider，它是 Spring Security 内置的）
         * DaoAuthenticationProvider 内部调用 UserDetailsServiceImpl.loadUserByUsername() 获取用户
         * 用 PasswordEncoder.matches() 比对密码
         * 返回一个已认证的 Authentication，或抛出异常
         */
    }
}
