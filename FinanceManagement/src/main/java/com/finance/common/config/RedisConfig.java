package com.finance.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis配置
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义序列化
     * Spring默认提供一个StringRedisTemplate类，它的key和value的序列化方式默认是String，可以省去我们自定义RedisTemplate的过程
     * 使用StringRedisTemplate可以不用下面的自定义RedisTemplate
     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//        //创建RedisTemplate对象
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        //设置连接工厂
//        template.setConnectionFactory(factory);
//        //创建JSON序列化工具
//        //使用JSON序列化会大幅度占用空间，可以使用String序列化，要求只储存String类型的key和value，当储存Java对象，手动完成序列化和反序列化
//        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
//        //设置Key的序列化
//        template.setKeySerializer(RedisSerializer.string());
//        template.setHashKeySerializer(RedisSerializer.string());
//        //设置value的序列化
//        template.setValueSerializer(jsonRedisSerializer);
//        template.setHashValueSerializer(jsonRedisSerializer);
//        //返回
//        return template;
//    }

    /**
     *常用操作速查表:
     * 操作类型	  Value	   Hash	   List	                Set	          ZSet
     * 新增	      set	   put	   leftPush/rightPush	add	           add
     * 查询	      get	   get	   index/range	        members	       range/reverseRange
     * 修改	      set	   put	   set	                add（自动去重）  add（覆盖)
     * 删除	      delete   delete  remove	            remove	       remove
     * 判断存在	  hasKey   hasKey	 -	                isMember	     -
     * 获取数量    size	   size	   size	                size	       zCard
     */
}
