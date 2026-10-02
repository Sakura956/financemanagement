package com.finance.modules.agent.tools;

import cn.hutool.core.collection.CollUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Agent 工具写入数据后的缓存清理
 *
 * 说明：现有 BillServiceImpl 在创建/修改/删除账单后会清理
 * "cache:bill:list:{userId}:*" 和 "cache:statistics:*:{userId}:*" 两类 Redis 缓存。
 * Agent 工具直接操作 Mapper（绕开了内部依赖 SecurityUtil 的 Service），
 * 因此这里按同样的 Key 规则清理缓存，避免 AI 记账后用户看到过期统计数据。
 */
@Component
public class AgentCacheCleaner {

    private final StringRedisTemplate stringRedisTemplate;

    public AgentCacheCleaner(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 清理当前用户的所有账单列表缓存 + 统计数据缓存
     */
    public void clearBillAndStatsCache(Long userId) {
        deleteByPattern("cache:bill:list:" + userId + ":*");
        deleteByPattern("cache:statistics:*:" + userId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (CollUtil.isNotEmpty(keys)) {
            stringRedisTemplate.delete(keys);
        }
    }
}
