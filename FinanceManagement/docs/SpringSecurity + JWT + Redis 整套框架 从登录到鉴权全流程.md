# SpringSecurity + JWT + Redis 整套框架 —— 从登录到鉴权全流程

---

# 前言：这些类到底是谁"生"的？

下面这张表把整套框架涉及的所有类按"出身"分清楚：

## 0.1 Java 标准库（JDK 自带）的类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `Date` | `java.util.Date` | Java 最基础的日期时间类 |
| `RuntimeException` | `java.lang.RuntimeException` | Java 异常体系中的运行时异常基类 |
| `Collections` | `java.util.Collections` | Java 集合工具类，提供 `singletonList()` 等静态方法 |
| `TimeUnit` | `java.util.concurrent.TimeUnit` | 时间单位枚举（SECONDS, DAYS 等） |

## 0.2 Spring Security 框架自带的类

| 类名 | 作用                                                       |
|------|----------------------------------------------------------|
| `UserDetailsService` | **接口**：定义"根据用户名加载用户信息"的契约                                |
| `UserDetails` | **接口**：定义 Spring Security 需要的用户信息结构                      |
| `User` | **类**：Spring Security 内置的 `UserDetails` 接口默认实现           |
| `UsernamePasswordAuthenticationToken` | **类**：Spring Security 的"用户名密码认证令牌"，存当前登录用户信息             |
| `SimpleGrantedAuthority` | **类**：Spring Security 权限对象的简单实现                          |
| `SecurityContextHolder`  | **类**：Spring Security 的"安全上下文持有者"，全局获取当前登录用户             |
| `OncePerRequestFilter` | **抽象类**：Spring Web 提供的"每个请求只执行一次"的过滤器基类                  |
| `AuthenticationManager`  | **接口**：Spring Security 认证管理器，负责执行认证                      |
| `AuthenticationConfiguration`  | **类**：Spring Security 配置类，用于获取全局 `AuthenticationManager` |
| `PasswordEncoder`  | **接口**：密码编码器接口                                           |
| `BCryptPasswordEncoder`  | **类**：BCrypt 算法的 `PasswordEncoder` 实现                    |
| `SecurityFilterChain`  | **接口**：Spring Security 过滤器链                              |
| `AccessDeniedHandler`  | **接口**：403 无权限处理器                                        |
| `AccessDeniedException`  | **类**：Spring Security 的"拒绝访问"异常                          |
| `Authentication`  | **接口**：认证对象的核心接口                                         |
| `UsernameNotFoundException`  | **类**：Spring Security 的"用户名未找到"异常                        |
| `@EnableWebSecurity`  | **注解**：开启 Spring Security 的 Web 安全配置                     |

## 0.3 第三方库的类（非 Spring Security，非 JDK）

| 类名 | 来源 | 作用 |
|------|------|------|
| `Claims` | `io.jsonwebtoken.Claims`（jjwt 库） | JWT 的 payload（载荷）对象，类似 Map |
| `Jwts` | `io.jsonwebtoken.Jwts`（jjwt 库） | JWT 构建器和解析器的入口类 |
| `Keys` | `io.jsonwebtoken.security.Keys`（jjwt 库） | 生成 JWT 签名密钥的工具类 |
| `SecretKey` | `javax.crypto.SecretKey`（JDK） | Java 加密扩展中的密钥接口 |
| `JSONUtil` | `cn.hutool.json.JSONUtil`（Hutool） | JSON 序列化/反序列化工具 |
| `BooleanUtil` | `cn.hutool.core.util.BooleanUtil`（Hutool） | Boolean 类型工具类 |
| `StrUtil` | `cn.hutool.core.util.StrUtil`（Hutool） | 字符串工具类 |

## 0.4 项目自定义的类（com.finance 包下自己写的）

| 类名 | 文件位置 | 职责 |
|------|---------|------|
| `SecurityConfig` | `security/SecurityConfig.java` | Spring Security 总配置类 |
| `JwtTokenProvider` | `security/JwtTokenProvider.java` | JWT 生成/解析/校验工具 |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` | 每次请求拦截校验 JWT 的过滤器 |
| `UserDetailsServiceImpl` | `security/UserDetailsServiceImpl.java` | 根据手机号查数据库返回用户信息 |
| `CustomAccessDeniedHandler` | `security/CustomAccessDeniedHandler.java` | 403 无权限的 JSON 响应处理器 |
| `SecurityUtil` | `util/SecurityUtil.java` | 业务层获取当前登录用户的静态工具类 |
| `BusinessException` | `common/exception/BusinessException.java` | 自定义业务异常，携带错误码 |
| `GlobalExceptionHandler` | `common/exception/GlobalExceptionHandler.java` | 全局异常捕获，统一返回 Result |
| `Result` | `common/result/Result.java` | 统一响应体 `{code, message, data}` |
| `RedisUtil` | `util/RedisUtil.java` | Redis 缓存读写工具 |
| `User` | `modules/auth/entity/User.java` | 数据库用户实体（MyBatis-Plus），对应 `sys_user` 表 |

---

# 一、类详解：每个类的继承链、作用、使用方式与注意点

> 下面按 **"从底层到上层"** 的顺序逐一剖析每个类，重点标注它的继承/实现关系。

---

## 1.1 User —— 数据库用户实体

```
继承链：Object（Java 原生）
注解：  @Data（Lombok）+ @TableName("sys_user")（MyBatis-Plus）
出身：  项目自定义
```

```java
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;         // 主键自增
    private String phone;    // 手机号（登录用）
    private String password; // BCrypt 加密后的密码
    private String nickname; // 昵称
    private String role;     // "ADMIN" 或 "USER"
    private Integer status;  // 0=正常, 1=被封禁
    private String avatarUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**作用**：对应数据库 `sys_user` 表，MyBatis-Plus 通过 `UserMapper`（继承 `BaseMapper<User>`）自动提供 CRUD 方法。

**注意点**：
- `role` 字段存的是 `"ADMIN"` 或 `"USER"`（**不带** `ROLE_` 前缀）。`ROLE_` 前缀是后面在组装 Security 权限对象时手动拼上去的。
- `status` 字段：`0` 正常，`1` 封禁。在 `UserDetailsServiceImpl` 中会据此设置 `enabled` 属性。
- `password` 存的是 BCrypt 加密后的密文，不是明文。

---

## 1.2 Result —— 统一响应体

```
继承链：Object（Java 原生）
出身：  项目自定义
```

```java
@Data
public class Result<T> {
    private int code;       // HTTP 状态码风格（200, 400, 401, 403, 500 等）
    private String message; // 提示信息
    private T data;         // 泛型数据体

    // 静态工厂方法
    public static <T> Result<T> success() { ... }                    // code=200
    public static <T> Result<T> success(T data) { ... }              // code=200
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> error(int code, String message) { ... }  // data=null
}
```

**作用**：所有接口统一返回 `{"code":200, "message":"登录成功", "data":{...}}` 格式。

**注意点**：
- 登录成功时返回 `Result.success("登录成功", loginResponse)`，其中 `loginResponse` 包含 token 和 userInfo。
- 登录失败时在 Service 中 `throw new BusinessException(401, "手机号或密码错误")`，由 `GlobalExceptionHandler` 转为 `Result.error(401, ...)`。
- 不是 Spring Security 的类，只是项目自己的响应封装。

---

## 1.3 BusinessException —— 自定义业务异常

```
继承链：RuntimeException（Java 原生）
         └── BusinessException（项目自定义）
出身：  项目自定义
```

```java
public class BusinessException extends RuntimeException {
    private final int code;  // 错误码（400, 401, 403, 404, 409, 500 等）

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {  // 默认 code=400
        super(message);
        this.code = 400;
    }
}
```

**作用**：让业务代码抛出一个带错误码的异常，然后由 `GlobalExceptionHandler` 统一捕获并转成 `Result.error(code, message)`。

**注意点**：
- 它是 `RuntimeException`，所以不需要在方法签名上 `throws`。
- 项目中用它抛 401（未登录/密码错误）、403（被封禁）、404（用户不存在）、409（手机号已注册）等。
- **它不是 Spring Security 的 `AccessDeniedException`**——那是框架层权限不足时自动抛的，不归这个类管。

---

## 1.4 GlobalExceptionHandler —— 全局异常处理

```
继承链：Object（Java 原生）
注解：  @RestControllerAdvice（Spring MVC）
出身：  项目自定义
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 业务异常 → Result.error(code, message)
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(...) {
        // @Valid 参数校验失败 → Result.error(400, 字段级错误信息)
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(...) {
        // 非法参数 → Result.error(400, ...)
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 兜底：未知异常 → Result.error(500, "服务器内部错误")
    }
}
```

**作用**：所有 Controller 抛出的异常都在这里被拦截并转成统一的 JSON 格式返回给前端。

**注意点**：
- `@RestControllerAdvice` 是 Spring MVC 提供的注解，不是 Spring Security 的。
- 这里**没有**专门捕获 `AuthenticationException` 或 `AccessDeniedException`（Spring Security 的异常）。这两个异常发生在过滤器链中（还没到 Controller），`@RestControllerAdvice` 拦不到它们，需要另外配置 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`。
- 项目中目前通过 `SecurityUtil.getCurrentUserId()` 抛 `BusinessException(401)` 来间接提示未登录，而不是走标准的 `AuthenticationEntryPoint`。

---

## 1.5 RedisUtil —— Redis 缓存工具

```
继承链：Object（Java 原生）
注解：  @Component（Spring 的通用组件注解）
出身：  项目自定义
```

```java
@Component
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) { ... }
    public String get(String key) { ... }
    public void delete(String key) { ... }
    public boolean hasKey(String key) { ... }
    public void expire(String key, long timeout, TimeUnit unit) { ... }
}
```

**作用**：封装了 `StringRedisTemplate` 的常用操作。在本项目中专门用于 JWT Token 的增删查。

**在安全流程中的用法**：
- 登录时：`redisUtil.set("token:user:" + userId, token, 7天)` —— 把 Token 存起来
- 鉴权时：`redisUtil.get("token:user:" + userId)` —— 取出来和请求头里的比对
- 退出/改密/封禁时：`redisUtil.delete("token:user:" + userId)` —— 删掉使其立即失效

**注意点**：
- `StringRedisTemplate` 是 Spring Data Redis 提供的，操作的都是字符串。
- key 的命名格式是 `token:user:{userId}`，用冒号分隔在 Redis GUI 中会显示为层级结构。
- TTL 是 7 天（`7 * 24 * 3600` 秒），和 JWT 的过期时间设置为一致。

---

## 1.6 PasswordEncoder —— 密码编码器接口

```
继承链：PasswordEncoder（接口，Spring Security 提供）
         └── BCryptPasswordEncoder（实现类，Spring Security 提供）
出身：  Spring Security 框架
```

```java
// 在 SecurityConfig 中注册为 Bean
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**接口方法**：

| 方法 | 作用 |
|------|------|
| `encode(rawPassword)` | 把明文密码加密成密文 |
| `matches(rawPassword, encodedPassword)` | 比对明文和密文是否匹配 |
| `upgradeEncoding(encodedPassword)` | 是否需要升级编码（用于密码策略升级） |

**使用位置**：
- **注册时**调用 `passwordEncoder.encode(request.getPassword())` 将明文加密存入数据库。
- **登录时**调用 `passwordEncoder.matches(request.getPassword(), user.getPassword())` 比对密码。
- **改密时**同样调用上述两个方法。

**注意点**：
- `BCryptPasswordEncoder` 每次对同一明文生成的密文都**不一样**（因为内置随机 salt），所以**不能用 `encode()` 的结果直接和数据库比较**，必须用 `matches()`。
- BCrypt 密文固定 60 个字符，以 `$2a$` 或 `$2b$` 开头。
- 注册时必须 encode，不能存明文。如果数据库存的明文，Security 的自动比对会失败。

---

## 1.7 UserDetailsService —— 用户详情加载接口

```
继承链：UserDetailsService（接口，Spring Security 提供）
         └── UserDetailsServiceImpl（项目自定义实现类）
出身：  UserDetailsService 是 Spring Security 框架自带的接口
       UserDetailsServiceImpl 是项目自定义的实现类
```

```java
// Spring Security 自带的接口，只有一个方法：
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

```java
// 项目实现
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 1. 根据手机号查数据库
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        // 2. 查不到抛异常（Spring Security 会处理）
        if (user == null) {
            throw new UsernameNotFoundException("手机号或密码错误");
        }

        // 3. 查到则组装 Spring Security 的 User 对象
        return new org.springframework.security.core.userdetails.User(
            String.valueOf(user.getId()),    // username → 存 userId
            user.getPassword(),              // password → 数据库 BCrypt 密文
            user.getStatus() != 1,           // enabled → false 则被禁用
            true,   // accountNonExpired   → 不过期
            true,   // credentialsNonExpired → 密码不过期
            true,   // accountNonLocked    → 未锁定
            Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())) // 权限
        );
    }
}
```

**作用**：Spring Security 在认证时自动调用此方法，根据登录名（本项目是手机号）从数据库加载用户信息，返回 `UserDetails` 对象。Spring Security 再自动拿这个对象和用户提交的密码做比对。

**什么时候被调用？**
在 `AuthServiceImpl.login()` 中手动调用了 `authenticationManager.authenticate(...)`，Security 内部就会自动找到这个 `UserDetailsServiceImpl.loadUserByUsername()`。

**注意点**：
- `loadUserByUsername` 的参数名叫 "username" 但本项目传的是手机号，没有问题——Spring Security 不关心它到底是什么，你传入什么它就用什么去查。
- 返回的 `User` 对象中第 1 个参数 `username` 存的是 `String.valueOf(user.getId())`，这样在后续整个请求生命周期中，`authentication.getPrincipal()` 拿到的就是 userId。
- 返回的 `User` 是 `org.springframework.security.core.userdetails.User`，**不是**项目的 `com.finance.modules.auth.entity.User`（两个同名不同包！这是常见的困惑点）。
- 本项目 `accountNonExpired`、`credentialsNonExpired`、`accountNonLocked` 都写死为 `true`，如果以后需要实现"账号过期""密码过期""账号锁定"功能，需要修改这里。

---

## 1.8 UserDetails —— 用户详情接口

```
继承链：UserDetails（接口，Spring Security 提供）
实现类：org.springframework.security.core.userdetails.User（Spring Security 提供）
         └── 这是框架内置的唯一官方实现，我们直接 new 它使用
出身：  Spring Security 框架
```

```java
// Spring Security 自带的接口
public interface UserDetails extends Serializable {
    Collection<? extends GrantedAuthority> getAuthorities(); // 权限集合
    String getPassword();                                     // 密码
    String getUsername();                                     // 用户名
    boolean isAccountNonExpired();                            // 账号是否未过期
    boolean isAccountNonLocked();                             // 账号是否未锁定
    boolean isCredentialsNonExpired();                        // 凭证是否未过期
    boolean isEnabled();                                      // 是否启用
}
```

**Spring Security 内置的默认实现 `org.springframework.security.core.userdetails.User`**：

```java
// 构造方法（本项目使用的）
public User(String username, String password, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities)
```

**作用**：这是 Spring Security 中描述"一个可以认证的用户"的标准数据结构。Spring Security 内部的所有认证流程都围绕这个接口进行。

**注意点**：
- 这个 `User`（`o.s.s.core.userdetails.User`）和项目的数据库实体 `User`（`com.finance.modules.auth.entity.User`）**完全不同**！前者是 Security 的认证用户模型，后者是数据库 ORM 实体。
- 本项目没有自己实现 `UserDetails` 接口，而是直接 `new` 框架内置的 `User`。对于大多数项目来说这就够用了。
- `enabled = false` 时，Spring Security 会自动阻止登录并抛出 `DisabledException`。

---

## 1.9 SimpleGrantedAuthority —— 简单权限对象

```
继承链：GrantedAuthority（接口，Spring Security 提供）
         └── SimpleGrantedAuthority（实现类，Spring Security 提供）
出身：  Spring Security 框架
```

```java
// 使用方式
new SimpleGrantedAuthority("ROLE_ADMIN")
new SimpleGrantedAuthority("ROLE_USER")
```

**作用**：包装一个权限字符串。Spring Security 用它来代表用户的"角色"或"权限"。

**注意点**：
- 字符串必须是 `ROLE_` 前缀开头，否则 `.hasRole("ADMIN")` 匹配不到。`.hasRole("ADMIN")` 内部会自动拼 `ROLE_` 前缀。
- `.hasAuthority("ROLE_ADMIN")` 则需要写完整的 `ROLE_ADMIN`。
- `getAuthority()` 方法返回的是完整字符串（如 `"ROLE_ADMIN"`），不是 `"ADMIN"`。

---

## 1.10 UsernamePasswordAuthenticationToken —— 用户名密码认证令牌

```
继承链：AbstractAuthenticationToken（抽象类，Spring Security 提供）
         └── UsernamePasswordAuthenticationToken（Spring Security 提供）
             实现了 Authentication 接口
出身：  Spring Security 框架
```

**两种用法（同一个类，两种构造方式）**：

```java
// 用法 1：认证前（未认证状态）— 传入用户名和密码
// 用于登录时交给 AuthenticationManager 去验证
new UsernamePasswordAuthenticationToken(phone, rawPassword);

// 用法 2：认证后（已认证状态）— 传入 principal + authorities
// 用于过滤器设置"当前用户已登录"
new UsernamePasswordAuthenticationToken(principal, null, authorities);
```

**在本项目中的使用**：

| 位置 | 方式 | 代码 |
|------|------|------|
| `AuthServiceImpl.login()` | 用法 1（认证前） | `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(phone, rawPassword))` |
| `JwtAuthenticationFilter.doFilterInternal()` | 用法 2（认证后） | `new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))` |

**注意点**：
- 两个构造方法的核心区别：**两参数构造**会设置 `authenticated = false`（未认证），**三参数构造**会设置 `authenticated = true`（已认证）。
- 当使用三参数构造时，传给 `SecurityContextHolder` 后，整个请求链路里都能通过 `SecurityUtil` 拿到当前用户。
- `principal`（第一个参数）存的是 `Long userId`，而不是用户名。这是本项目的设计选择。

---

## 1.11 SecurityContextHolder —— 安全上下文持有者

```
继承链：Object（Java 原生）
         └── SecurityContextHolder（Spring Security 提供，纯静态工具类）
出身：  Spring Security 框架
```

```java
// 核心 API
SecurityContextHolder.getContext().setAuthentication(auth);  // 存入当前用户
SecurityContextHolder.getContext().getAuthentication();      // 取出当前用户
SecurityContextHolder.clearContext();                        // 清除上下文
```

**作用**：Spring Security 把当前登录用户的信息存在 `ThreadLocal` 里（默认策略 `MODE_THREADLOCAL`），整个请求链路中（同一线程）任何地方都能拿到。

**存储策略（如何存）**：
- `MODE_THREADLOCAL`（默认）：存在当前线程的 ThreadLocal 中，请求结束自动清理。
- `MODE_INHERITABLETHREADLOCAL`：子线程也能继承。
- `MODE_GLOBAL`：全局共享（不推荐）。

**注意点**：
- 本项目是无状态 JWT 模式，`SecurityContextHolder` 只在**当次请求**内有效。请求结束后 `ThreadLocal` 会被清理，下一个请求需要重新走 `JwtAuthenticationFilter` 设置。
- 如果在 Service 层开了新线程（如 `@Async`），子线程里 `SecurityContextHolder.getContext()` 拿不到数据，需要显式传递或用 `MODE_INHERITABLETHREADLOCAL`。

---

## 1.12 OncePerRequestFilter —— 每次请求只执行一次的过滤器

```
继承链：GenericFilterBean（抽象类，Spring Web 提供）
         └── OncePerRequestFilter（抽象类，Spring Web 提供）
              └── JwtAuthenticationFilter（项目自定义）
出身：  OncePerRequestFilter 是 Spring Web 框架自带的（不是 Spring Security）
       JwtAuthenticationFilter 是项目自定义的
```

```java
// OncePerRequestFilter 的核心抽象方法（需要子类重写）
protected abstract void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException;
```

**为什么用 `OncePerRequestFilter` 而不是普通 `Filter`？**
- `.addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` 这种方式会把过滤器注册到 Spring Security 的过滤器链中。
- 如果请求在内部被 `forward` 或 `include` 到另一个 Servlet，普通 `Filter` 可能执行多次。
- `OncePerRequestFilter` 保证**每个 HTTP 请求**只执行一次。

**注意点**：
- 它继承自 `GenericFilterBean`，而 `GenericFilterBean` 实现了 `jakarta.servlet.Filter`（Jakarta EE 的 Filter 接口），所以它本质上是一个 Servlet Filter。
- 本项目重写的 `doFilterInternal` **不管校验成功还是失败都会调用 `filterChain.doFilter(request, response)` 放行**。这是因为即使没 Token，SecurityConfig 的 `.permitAll()` 或 `.authenticated()` 规则会在后续链中判断是否拦截。
- 不要在 `doFilterInternal` 中 `return` 提前结束——一旦提前 return，后面的过滤器链就断了。

---

## 1.13 JwtAuthenticationFilter —— JWT 认证过滤器

```
继承链：GenericFilterBean（Spring Web）
         └── OncePerRequestFilter（Spring Web）
              └── JwtAuthenticationFilter（项目自定义）
出身：  项目自定义
```

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 步骤1：从请求头提取 Token
        String token = resolveToken(request);  // 从 "Authorization: Bearer xxx" 中截取

        // 步骤2：Token 非空且签名有效
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // 步骤3：解析 Token 中的 userId 和 role
            Claims claims = jwtTokenProvider.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            // 步骤4：校验 Redis 中是否存在（防止退出登录后 Token 仍可用）
            String redisToken = redisUtil.get("token:user:" + userId);
            if (redisToken != null && redisToken.equals(token)) {

                // 步骤5：组装认证对象并存入 SecurityContextHolder
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userId,      // principal
                        null,        // credentials（已认证不需要密码）
                        Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role))
                    );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 步骤6：无论如何都放行，后续由 SecurityConfig 的规则决定是否拦截
        filterChain.doFilter(request, response);
    }

    // 从请求头提取 Bearer Token
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);  // 截掉 "Bearer " 这 7 个字符
        }
        return null;
    }
}
```

**如何注册到 Spring Security 中**（在 `SecurityConfig` 中）：

```java
http.addFilterBefore(
    new JwtAuthenticationFilter(jwtTokenProvider, redisUtil),
    UsernamePasswordAuthenticationFilter.class
);
```

这句话的意思是：把 `JwtAuthenticationFilter` 插入到 Spring Security 自带的 `UsernamePasswordAuthenticationFilter` **之前**。这样在 Spring Security 处理表单登录之前，我们的 JWT Filter 就已经解析完 Token 并设置了认证状态。

**注意点**：
- 这个过滤器**不是 `@Component`**，而是 `SecurityConfig` 中手动 `new` 的。这样做是为了不让它被 Spring Boot 自动注册为全局 Filter（全局 Filter 会对所有请求生效，包括静态资源，而放在 Security 过滤器链中可以被 Security 的忽略规则排除）。
- Redis 校验是"双重保险"：JWT 签名没过期，但如果 Redis 中 token 被删了（比如退出登录），则认证失败。
- `StringUtils.hasText()` 来自 `org.springframework.util.StringUtils`（Spring 框架自带的工具类）。

---

## 1.14 SecurityContextHolder 与 JwtAuthenticationFilter 的协作关系

这是理解整个流程最关键的部分：

```
请求进来
    │
    ▼
JwtAuthenticationFilter.doFilterInternal()
    │
    ├── Token 无效/不存在 → 什么都不做
    │
    └── Token 有效 + Redis 校验通过 → SecurityContextHolder.getContext().setAuthentication(auth)
                                          │
                                          ▼
                              把 userId + role 存入当前线程的 ThreadLocal
                                          │
                                          ▼
                              继续执行 filterChain.doFilter()
                                          │
                                          ▼
                              进入 SecurityConfig 规则匹配（.hasRole("ADMIN") / .authenticated()）
                                          │
                                          ▼
                              进入 Controller → Service
                                          │
                                          ▼
                              SecurityUtil.getCurrentUserId() 从 ThreadLocal 取 userId
                                          │
                                          ▼
                              请求结束 → Tomcat 清理 ThreadLocal
```

---

## 1.15 Authentication —— 认证对象接口

```
继承链：Principal（java.security.Principal，Java 原生）
         └── Authentication（接口，Spring Security 提供）
              └── AbstractAuthenticationToken（抽象类，Spring Security）
                   └── UsernamePasswordAuthenticationToken（实现类，Spring Security）
出身：  Spring Security 框架
```

```java
public interface Authentication extends Principal, Serializable {
    Collection<? extends GrantedAuthority> getAuthorities(); // 权限
    Object getCredentials();    // 凭证（密码）
    Object getDetails();        // 额外详情（如 IP 地址）
    Object getPrincipal();      // 主体（本项目是 userId）
    boolean isAuthenticated();  // 是否已认证
    void setAuthenticated(boolean isAuthenticated);
}
```

**注意点**：
- `getPrincipal()` 返回的内容取决于你 `new UsernamePasswordAuthenticationToken` 时传入的第一个参数是什么。本项目传的是 `Long userId`，所以取出来也是 `Long`。
- 如果你传入 `UserDetails` 对象作为 principal，那取出来的就是 `UserDetails` 实例。

---

## 1.16 AuthenticationManager —— 认证管理器

```
继承链：AuthenticationManager（接口，Spring Security 提供）
         └── ProviderManager（默认实现，Spring Security 提供）
出身：  Spring Security 框架
```

```java
// 在 SecurityConfig 中暴露为 Bean
@Bean
public AuthenticationManager authenticationManager(
        AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

**核心方法**：

```java
Authentication authenticate(Authentication authentication) throws AuthenticationException;
```

**内部工作流程**：
1. 接收一个未认证的 `Authentication`（本项目是 `UsernamePasswordAuthenticationToken`）
2. 迭代所有注册的 `AuthenticationProvider`
3. 找到能处理该类型的 `Provider`（本项目用的是 `DaoAuthenticationProvider`，它是 Spring Security 内置的）
4. `DaoAuthenticationProvider` 内部调用 `UserDetailsServiceImpl.loadUserByUsername()` 获取用户
5. 用 `PasswordEncoder.matches()` 比对密码
6. 返回一个已认证的 `Authentication`，或抛出异常

**在本项目中的使用**：

```java
// AuthServiceImpl.login() 中
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(phone, rawPassword)
);
```

**注意点**：
- 本项目调用了 `authenticate()` 但**没有用它的返回值**，而是自己又写了一次密码校验逻辑。这意味着 `AuthenticationManager` 在本项目中实际上只是一个"流程触发者"，Security 的自动密码比对被执行了两次（一次在  authenticate() 内部，一次在 login() 方法的手动 matches()）。
- 如果要充分利用 `AuthenticationManager`，可以用它的返回值来判断认证成功与否，而不是自己再查一次数据库。

---

## 1.17 SecurityFilterChain —— 安全过滤器链

```
继承链：SecurityFilterChain（接口，Spring Security 提供）
         └── DefaultSecurityFilterChain（Spring Security 内置的默认实现）
出身：  Spring Security 框架
```

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. 禁用 CSRF（因为是无状态 API，没有 Session，CSRF 攻击无从谈起）
        .csrf(csrf -> csrf.disable())

        // 2. 设为无状态（不创建 HTTP Session）
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 3. URL 权限规则
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/user/**").authenticated()
            .anyRequest().permitAll()
        )

        // 4. 403 自定义处理器
        .exceptionHandling(ex ->
            ex.accessDeniedHandler(accessDeniedHandler))

        // 5. 把 JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider, redisUtil),
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
}
```

**URL 权限规则详解**：

| 配置 | 含义 | 实际效果 |
|------|------|---------|
| `.permitAll()` | 任何人都能访问 | 不需要 Token 也能请求 |
| `.authenticated()` | 必须登录 | 需要有有效的 Authentication（JWT Filter 设置了即可） |
| `.hasRole("ADMIN")` | 必须有 ADMIN 角色 | Authentication 的 authorities 中必须有 `ROLE_ADMIN` |
| `.hasRole("USER")` | 必须有 USER 角色 | Authentication 的 authorities 中必须有 `ROLE_USER` |
| `.anyRequest().permitAll()` | 其余全部放行 | 兜底规则，匹配未被前面规则覆盖的 URL |

**注意点**：
- `requestMatchers` 的顺序很重要：**更具体的规则写前面，更宽泛的写后面**。
- `.hasRole("ADMIN")` 内部会自动加 `ROLE_` 前缀，所以它匹配的是 `ROLE_ADMIN`。
- `SessionCreationPolicy.STATELESS` 表示不创建也不使用 HTTP Session，每次请求都是独立的——这正是 JWT 认证模式的核心配置。
- CSRF 必须关闭！CSRF 防护依赖 Session，无状态模式下不适用。

---

## 1.18 AccessDeniedHandler —— 拒绝访问处理器

```
继承链：AccessDeniedHandler（接口，Spring Security 提供）
         └── CustomAccessDeniedHandler（项目自定义实现类）
出身：  AccessDeniedHandler 是 Spring Security 框架自带的接口
       CustomAccessDeniedHandler 是项目自定义的实现类
```

```java
// Spring Security 自带的接口
public interface AccessDeniedHandler {
    void handle(HttpServletRequest request, HttpServletResponse response,
                AccessDeniedException accessDeniedException)
            throws IOException, ServletException;
}
```

```java
// 项目实现
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(403);
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(403, "无权限")));
    }
}
```

**什么时候被触发？**
当一个**已经登录的**用户访问了他没权限的接口时（如普通用户访问 `/api/v1/admin/**`），`SecurityFilterChain` 直接拦截，不会进入 Controller，而是调用这个 Handler。

**返回给前端**：
```json
{"code": 403, "message": "无权限", "data": null}
```

**注意点**：
- **已登录但没权限** → 触发 `AccessDeniedHandler`（403）
- **根本没登录** → 触发 `AuthenticationEntryPoint`（401）——本项目没有配置这个，所以未登录访问时返回的是 Spring Security 默认的 403 或报错页面。
- 注释掉 `exceptionHandling` 配置时，Spring Security 会返回默认的 403 空白页或英文错误信息，而不是 JSON，前端无法解析。
- 这个 Handler 不能抛异常给 `GlobalExceptionHandler`，因为它发生在过滤器层，不在 Controller 层。必须像上面一样直接 `response.getWriter().write()` 输出 JSON。

---

## 1.19 SecurityConfig —— 安全总配置

```
继承链：Object（Java 原生）
注解：  @Configuration + @EnableWebSecurity（Spring Security 提供）
出身：  项目自定义
```

这是整个安全模块的"总控室"，它做了 5 件事情：

| 职责 | 对应代码 |
|------|---------|
| 暴露 `PasswordEncoder` Bean | `@Bean public PasswordEncoder passwordEncoder()` |
| 暴露 `AuthenticationManager` Bean | `@Bean public AuthenticationManager authenticationManager(...)` |
| 配置 URL 权限规则 | `.authorizeHttpRequests(...)` |
| 配置无状态 Session | `.sessionManagement(...)` |
| 注册自定义过滤器 | `.addFilterBefore(new JwtAuthenticationFilter(...), ...)` |
| 注册 403 处理器 | `.exceptionHandling(ex -> ex.accessDeniedHandler(...))` |

**两个关键注解**：

- `@Configuration`：告诉 Spring 这是一个配置类，里面的 `@Bean` 方法会被 Spring 管理。
- `@EnableWebSecurity`：开启 Spring Security 的 Web 安全功能。它会自动导入 Spring Security 的默认配置。

**注意点**：
- Spring Boot 3.x（本项目用的是 3.4.4）中，`SecurityFilterChain` 是 Bean 定义的推荐方式，不再需要继承 `WebSecurityConfigurerAdapter`（该类在 Spring Security 5.7+ 已弃用，6.x/3.x 中已移除）。
- `JwtAuthenticationFilter` 没有加 `@Component`，而是在这里手动 `new`，防止它被当作全局 Filter 注册。

---

## 1.20 JwtTokenProvider —— JWT 工具类

```
继承链：Object（Java 原生）
注解：  @Component（Spring 的通用组件注解）
出身：  项目自定义
依赖库：io.jsonwebtoken（jjwt 0.12.6）
```

```java
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;   // HMAC 签名密钥
    private final long expiration;       // 过期时间（毫秒）

    // 构造时从 application.yml 读取 jwt.secret 和 jwt.expiration
    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    // ========== 核心方法 ==========

    // 1. 生成 Token
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claim("userId", userId)           // 自定义 payload
                .claim("role", role)               // 自定义 payload
                .issuedAt(now)                     // 签发时间 (iat)
                .expiration(expiryDate)            // 过期时间 (exp)
                .signWith(secretKey)               // HMAC-SHA 签名
                .compact();                        // 生成最终字符串
    }

    // 2. 解析 Token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)             // 用同一密钥验证签名
                .build()
                .parseSignedClaims(token)           // 解析签名后的 claims
                .getPayload();                      // 获取 payload
    }

    // 3. 从 Token 中提取 userId
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    // 4. 从 Token 中提取 role
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    // 5. 校验 Token 是否有效
    public boolean validateToken(String token) {
        try {
            parseToken(token);    // 能解析成功 = 签名有效 + 未过期
            return true;
        } catch (Exception e) {
            return false;         // 任何异常都算无效
        }
    }
}
```

**JWT Token 结构解析**：

一个 JWT Token 由三部分组成，用 `.` 分隔：`header.payload.signature`

```
Header（Base64 解码后）:
{
  "alg": "HS256"   ← HMAC-SHA256 签名算法
}

Payload（Base64 解码后）:
{
  "userId": 1,
  "role": "ADMIN",
  "iat": 1700000000,   ← 签发时间戳（秒）
  "exp": 1706048000    ← 过期时间戳（秒）= iat + 604800000ms (7天)
}

Signature:
HMAC-SHA256(header + "." + payload, secretKey)
```

**注意点**：
- `secretKey` 必须足够长（至少 256 bits = 32 字节），否则 `Keys.hmacShaKeyFor()` 会抛异常。配置中的 `FinanceManagement-JWT-Secret-Key-2026-YourSecretKeyHere-MustBeAtLeast256Bits` 长度足够。
- `validateToken()` 的异常捕获太宽泛（catch Exception），把过期、签名错误、格式错误等全部归为"无效"，生产环境建议细化异常类型方便排查。
- `expiration` 配置值为 `604800000`（毫秒），换算 = 604800 秒 = 10080 分钟 = 168 小时 = **7 天**。
- jjwt 0.12.x 的 API 和 0.11.x 完全不同：`Jwts.parser()` 替代了 `Jwts.parserBuilder()`，`verifyWith()` 替代了 `setSigningKey()`，`parseSignedClaims()` 替代了 `parseClaimsJws()`。

---

## 1.21 SecurityUtil —— 业务层获取当前用户工具

```
继承链：Object（Java 原生）
        所有方法都是 static，不依赖 Spring 容器
出身：  项目自定义
```

```java
public class SecurityUtil {

    // 获取当前登录用户 ID
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 三个条件任一不满足就抛 401
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(401, "未登录或Token已过期");
        }
        return (Long) auth.getPrincipal();
    }

    // 获取当前用户角色
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未登录或Token已过期");
        }
        return auth.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)     // 返回 "ROLE_ADMIN" 或 "ROLE_USER"
                .orElse("USER");
    }

    // 判断是否管理员
    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }
}
```

**使用的场景**（项目中实际调用位置）：

| 位置 | 调用 |
|------|------|
| `AuthServiceImpl.changePassword()` | `SecurityUtil.getCurrentUserId()` —— 获取要改密码的用户 ID |
| `AuthServiceImpl.me()` | `SecurityUtil.getCurrentUserId()` —— 查询当前用户信息 |
| `AuthServiceImpl.updateProfile()` | `SecurityUtil.getCurrentUserId()` —— 更新当前用户资料 |
| `FileStorageService.deleteOldAvatar()` | `SecurityUtil.getCurrentUserId()` —— 上传头像前删旧头像 |
| `AdminUserServiceImpl.updateUserStatus()` | `SecurityUtil.getCurrentUserId()` —— 禁止封禁自己 |
| 各个 Service 需要当前用户 ID 的地方 | `SecurityUtil.getCurrentUserId()` |

**注意点**：
- 这是一个纯 static 工具类，没有 `@Component` 注解，不需要注入，直接 `SecurityUtil.getCurrentUserId()` 即可。
- **⚠ 潜在 Bug**：`isAdmin()` 比较的是 `"ADMIN".equals(getCurrentUserRole())`，但 `getCurrentUserRole()` 返回的是 `"ROLE_ADMIN"`（因为 `SimpleGrantedAuthority.toString()` 返回的是 `"ROLE_ADMIN"`）。所以 `isAdmin()` **永远返回 false**。应该改为：
  ```java
  public static boolean isAdmin() {
      return "ROLE_ADMIN".equals(getCurrentUserRole());
  }
  // 或者
  public static boolean isAdmin() {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      return auth.getAuthorities().stream()
              .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }
  ```
- `"anonymousUser"` 是 Spring Security 在用户未登录时自动设置的默认 principal 字符串，不是你的 userId。检查它很重要，否则未登录时 `(Long) auth.getPrincipal()` 会抛 `ClassCastException`。

---

## 1.22 CacheClient —— 高级 Redis 缓存工具

```
继承链：Object（Java 原生）
注解：  @Component
出身：  项目自定义
```

```java
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    // 普通缓存写入（带过期时间）
    public void set(String key, Object value, Long time, TimeUnit unit) { ... }

    // 带"逻辑过期"的缓存写入（不设置 Redis TTL，用 JSON 中的 expireTime 字段判断过期）
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) { ... }

    // 缓存穿透保护 + 缓存空值（防止缓存穿透：查不到的数据也缓存一个空串）
    public <R, ID> R queryWithPassThrough(
        String keyPrefix, ID id, Class<R> type,
        Function<ID, R> doFallback,  // 缓存未命中时的数据库查询回调
        Long time, TimeUnit unit
    ) { ... }

    // 逻辑过期 + 互斥锁防止缓存击穿（热点数据过期时只让一个线程去重建缓存）
    public <R, ID> R queryWithLogicalExpire(
        String keyPrefix, ID id, Class<R> type,
        Function<ID, R> doFallback,
        Long time, TimeUnit unit
    ) { ... }
}
```

**注意点**：
- 这个类和 JWT Token 的存储用的是同一个 `StringRedisTemplate`，但它用的是 JSON 序列化方式存对象，而 JWT Token 是直接存字符串。
- `queryWithPassThrough` 处理了**缓存穿透**问题：查不到的数据在 Redis 中缓存一个空串 `""`（注意不是 `null`），这样下次请求同一 ID 时直接返回 null，不会打到数据库。
- `queryWithLogicalExpire` 处理了**缓存击穿**问题：用 Redis 的 `setIfAbsent` 作为分布式锁，保证只有一个线程去查数据库重建缓存。
- 这个类没有被登录/鉴权流程使用，它用于业务数据的缓存（如账单分类等）。

---

## 1.23 RedisData —— 逻辑过期包装对象

```
继承链：Object（Java 原生）
注解：  @Data（Lombok）
出身：  项目自定义
```

```java
@Data
public class RedisData {
    private LocalDateTime expireTime; // 逻辑过期时间
    private Object data;             // 实际缓存的数据
}
```

**作用**：配合 `CacheClient.setWithLogicalExpire()` 和 `queryWithLogicalExpire()` 使用。不是存一个直接的过期时间，而是把数据和时间包装在一起存进 Redis，读取时通过比较 `expireTime` 和当前时间来判断是否过期。

**注意点**：
- 和 JWT 鉴权无关，是业务缓存层面的工具类。

---

# 二、完整流程走一遍（从头到脚）

## 阶段 1：登录 —— 从账号密码到返回 Token

```
POST /api/v1/auth/login  { "phone": "13800138000", "password": "123456" }
    │
    ▼
AuthController.login()
    │
    ▼
AuthServiceImpl.login(request)
    │
    ├── [1] 根据手机号查数据库 → User 对象
    │       userMapper.selectOne(phone)
    │
    ├── [2] 手动密码校验
    │       passwordEncoder.matches(rawPassword, user.getPassword())
    │       ├── 不匹配 → throw BusinessException(401, "手机号或密码错误")
    │       └── 匹配 → 继续
    │
    ├── [3] 检查是否被封禁
    │       user.getStatus() == 1 → throw BusinessException(403, "账号已被封禁")
    │
    ├── [4] 生成 JWT
    │       jwtTokenProvider.generateToken(user.getId(), user.getRole())
    │       → 返回签名的 JWT 字符串
    │
    ├── [5] 存 Redis
    │       redisUtil.set("token:user:" + userId, token, 7天)
    │       → 用于后续在线状态校验
    │
    └── [6] 返回 LoginResponse { token, userInfo }
            前端存 Token → 后续所有请求头带 Authorization: Bearer {token}
```

## 阶段 2：鉴权 —— 每次请求拦截并设置认证信息

```
任意请求（如 GET /api/v1/user/bills）
    │
    ▼
JwtAuthenticationFilter.doFilterInternal(request, response, filterChain)
    │
    ├── [1] 提取 Token: resolveToken(request)
    │       request.getHeader("Authorization") → "Bearer xxx..."
    │       → 截取 "Bearer " 后的字符串
    │
    ├── [2] 判断有效性:
    │       if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token))
    │       ├── Token 为空或无效 → 跳过，直接放行
    │       └── Token 有效 → 继续
    │
    ├── [3] 解析 Token:
    │       Claims claims = jwtTokenProvider.parseToken(token)
    │       → userId, role
    │
    ├── [4] Redis 二次校验:
    │       redisToken = redisUtil.get("token:user:" + userId)
    │       ├── redisToken == null → Token 已失效（退出/改密/被封禁）
    │       ├── redisToken != token → Token 不匹配（用旧 Token 重新登录后仍可用）
    │       └── 匹配 → 继续
    │
    ├── [5] 组装认证对象并存入上下文:
    │       UsernamePasswordAuthenticationToken auth =
    │           new UsernamePasswordAuthenticationToken(userId, null, authorities)
    │       SecurityContextHolder.getContext().setAuthentication(auth)
    │
    └── [6] 放行:
            filterChain.doFilter(request, response)
            → 进入下一个过滤器 → 进入 SecurityConfig 权限规则匹配
```

## 阶段 3：授权 —— SecurityConfig 规则匹配

```
SecurityFilterChain.authorizeHttpRequests 规则匹配:
    │
    ├── 匹配到 .permitAll() → 直接进入 Controller
    │
    ├── 匹配到 .authenticated() → 检查 SecurityContext 中有没有认证对象
    │       ├── 有（JWT Filter 已设置）→ 进入 Controller
    │       └── 没有 → 拦截，返回 403（本项目没配 AuthenticationEntryPoint，走默认行为）
    │
    └── 匹配到 .hasRole("ADMIN") → 检查 authorities 中是否有 "ROLE_ADMIN"
            ├── 有 → 进入 Controller
            └── 没有 → CustomAccessDeniedHandler.handle() → JSON {"code":403, "message":"无权限"}
```

## 阶段 4：业务层获取当前用户

```
Controller / Service 中调用:
    │
    ├── SecurityUtil.getCurrentUserId()
    │       → SecurityContextHolder.getContext().getAuthentication().getPrincipal()
    │       → 返回 Long userId
    │
    └── SecurityUtil.getCurrentUserRole()
            → SecurityContextHolder.getContext().getAuthentication().getAuthorities()
            → 返回 "ROLE_ADMIN" 或 "ROLE_USER"
```

## 阶段 5：退出登录 / 密码变更 / 封禁 —— 让 Token 失效

```
修改密码 / 管理员封禁用户:
    │
    └── redisUtil.delete("token:user:" + userId)
            │
            ▼
        下次请求来时:
            JwtAuthenticationFilter → redisUtil.get("token:user:" + userId) → null
            → SecurityContextHolder 不会设置认证信息
            → 请求被拦截
```

---

# 三、类与类之间的依赖关系图

```
SecurityConfig (@Configuration + @EnableWebSecurity)
├── 依赖 JwtTokenProvider (@Component)           → JWT 操作
├── 依赖 RedisUtil (@Component)                  → Redis 操作
├── 依赖 CustomAccessDeniedHandler (@Component)  → 403 处理
├── 创建 JwtAuthenticationFilter                 → 每次请求拦截
│       ├── 依赖 JwtTokenProvider
│       ├── 依赖 RedisUtil
│       └── 使用 SecurityContextHolder           → 存储认证信息
├── 暴露 PasswordEncoder Bean (@Bean)            → BCryptPasswordEncoder
│       └── 被 AuthServiceImpl 注入使用
└── 暴露 AuthenticationManager Bean (@Bean)
        └── 被 AuthServiceImpl 注入使用
                │
                └── 内部自动调用 UserDetailsServiceImpl
                        └── 依赖 UserMapper (MyBatis-Plus)

AuthServiceImpl (@Service)
├── 依赖 UserMapper
├── 依赖 PasswordEncoder
├── 依赖 JwtTokenProvider
├── 依赖 RedisUtil
└── 使用 SecurityUtil (static)

SecurityUtil (纯静态工具类)
└── 依赖 SecurityContextHolder (Spring Security 静态类)

GlobalExceptionHandler (@RestControllerAdvice)
└── 捕获所有异常 → 返回统一 Result

CustomAccessDeniedHandler (@Component)
└── 实现 AccessDeniedHandler 接口 (Spring Security 提供)
```

---

# 四、配置文件分析

## 4.1 JWT 配置（application.yml / application-dev.yml / application-prod.yml）

```yaml
jwt:
  secret: FinanceManagement-JWT-Secret-Key-2026-YourSecretKeyHere-MustBeAtLeast256Bits
  expiration: 604800000  # 7 天 = 7 * 24 * 3600 * 1000 毫秒
```

读取方式：
```java
@Value("${jwt.secret}")     // 注入 secret 字符串
@Value("${jwt.expiration}") // 注入 expiration 毫秒值
```

## 4.2 Redis 配置

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ""
      database: 1          # dev 用 db 1, prod 用 db 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

**注意点**：
- Spring Boot 自动配置了 `StringRedisTemplate`，不需要手写 RedisConfig。
- 本项目的 `RedisConfig.java` 中自定义的 `RedisTemplate` 被注释掉了，实际用的就是 Spring Boot 默认的 `StringRedisTemplate`。

## 4.3 Spring Security 日志配置

```yaml
logging:
  level:
    com.finance: debug
    org.springframework.security: info   # dev 环境, prod 为 warn
```

把 `org.springframework.security` 的日志级别设为 `debug` 可以看到完整的过滤器链和认证过程，方便调试。
