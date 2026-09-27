package com.finance.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis操作工具
 * 封装常用的 Redis 操作方法：set、get、delete、expire、hasKey 等，简化 Redis 操作代码
 */
@Component
public class RedisUtil {

    /**
     * SpringDataRedis中提供了RedisTemplate工具类，其中封装了各种对Redis的操作，并且将不同数据类型的操作API封装到不同的类型中
     *
     * redisTemplate.opsForValue()  返回值类型：ValueOperations   操作String类型数据
     * redisTemplate.opsForHash()   返回值类型：HashOperations    操作Hash类型数据
     * redisTemplate.opsForList()   返回值类型：ListOperations    操作List类型数据
     * redisTemplate.opsForSet()    返回值类型：SetOperations     操作Set类型数据
     * redisTemplate.opsForZSet()   返回值类型：ZSetOperations    操作SortedSet类型数据
     * redisTemplate                通用命令
     *
     * 引入Redis依赖和连接池依赖（commons-pool2）
     * 配置相关信息
     * 使用方式如：redisTemplate.opsForValue().set("name","Redis")
     * RedisTemplate可以接收任意的Object作为值写入Redis，只不过写入前会把Object序列化成字符形式，默认采用JDK序列化
     * 可以自定义RedisTemplate的序列化方式，具体见Redis配置文件
     *
     * 使用JSON序列化会大幅度占用空间，可以使用String序列化，要求只储存String类型的key和value，当储存Java对象，手动完成序列化和反序列化
     * Spring默认提供一个StringRedisTemplate类，它的key和value的序列化方式默认是String，省去我们自定义RedisTemplate的过程
     *
     * 手动序列化和反序列化Java对象，比如：
     * private static final ObjectMapper mapper = new ObjectMapper();//序列化工具可以选择其他，此处为演示
     *
     * User user=new User("name",18);
     * String json = mapper.writeValueASString(user);//手动序列化
     * stringRedisTemplate.opsForValue().set("user:200",json);//写入数据
     * String jsonUser = stringRedisTemplate.opsForValue.get("user:200")//获取数据
     * User user1 = mapper.readValue(jsonUser,User.class);//手动反序列化
     *
     */

    //StringRedisTemplate 是 Spring 提供的专门操作 Redis 的类，它的key和value的序列化方式默认是String
    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 存数据（永久有效）
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 存数据（带过期时间）
     * @param key
     * @param value
     * @param timeout
     * @param unit
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        //最后一个参数是时间单位
        //TimeUnit.SECONDS  → 秒
        //TimeUnit.MINUTES → 分钟
        //TimeUnit.HOURS → 小时
        //TimeUnit.DAYS → 天
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }


    /**
     * 取数据
     * @param key
     * @return
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除数据
     * @param key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }


    /**
     * 判断 key 是否存在
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }


    /**
     * 设置过期时间
     * @param key
     * @param timeout
     * @param unit
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }
}
