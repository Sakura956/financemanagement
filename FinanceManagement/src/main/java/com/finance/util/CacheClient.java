package com.finance.util;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 封装Redis缓存处理工具
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 普通存缓存
     * 对象转 JSON 存入 Redis，设置真实 TTL 过期，普通缓存通用
     * @param key Redis 缓存 key
     * @param value 要存入的任意对象
     * @param time 过期时长数值
     * @param unit 时间单位 (秒 / 分 / 时)
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        //把对象转 JSON → 存入 Redis → 设置过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    /**
     * 逻辑过期存缓存
     * 封装数据 +逻辑过期时间存入 Redis
     * @param key 缓存 key
     * @param value 业务数据对象
     * @param time 逻辑过期时长
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //Redis 不设置过期，永远不过期,靠业务判断 expireTime 是否过期 ,用于高并发场景，避免缓存击穿

        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 查询缓存 + 缓存穿透防护（空值缓存）
     * @param keyPrefix 缓存 key 前缀
     * @param id 查询唯一标识 id
     * @param type 要转换成的实体 class
     * @param doFallback 函数式回调，缓存没命中时执行查库逻辑
     * @param time 缓存过期时长
     * @param unit 时间单位
     * @return
     * @param <R> 返回值实体类型
     * @param <ID> 主键 id 类型
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> doFallback,Long time, TimeUnit unit) {
        //拼装存储的key
        String key = keyPrefix + id;
        //查询Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //查询到有数据，直接返回
        if(StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json,type);
        }
        //如果是 "" 空值 ，则返回null，防止缓存穿透
        if(json!=null) {
            return null;
        }

        //数据库查询（函数式编程）
        R r =doFallback.apply(id);
        //查询为空，设置空值，防止缓存穿透
        if(r==null) {
            stringRedisTemplate.opsForValue().set(key,"",time, unit);
            return null;
        }
        //查询有结果，存入Redis
        this.set(key,r,time,unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    //获取锁/释放锁的方法
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 逻辑过期（高并发专用）判断逻辑时间过期 → 异步重建缓存 → 直接返回旧数据 → 高并发下无等待
     * @param keyPrefix key 前缀
     * @param id 业务 id
     * @param type 返回实体类型
     * @param doFallback 查库回调方法
     * @param time 逻辑过期时长
     * @param unit 时间单位
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID>R queryWithLogicalExpire(String keyPrefix,ID id,Class<R> type, Function<ID,R> doFallback,Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        //查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json)) {
            return null;
        }
        //把数据转成 RedisData
        RedisData redisData = JSONUtil.toBean(json,RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();

        //没过期 → 直接返回
        if(expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }

        String lockKey = "lock:shop:" + id;
        //过期了 → 获取锁
        boolean isLock = tryLock(lockKey);

        //拿到锁 → 开线程异步重建缓存
        if(isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = doFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unlock(key);
                }
            });
        }
        //直接返回旧数据,不等待！高并发性能爆炸！
        return r;
    }
}