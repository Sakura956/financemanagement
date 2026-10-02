package com.finance.modules.agent.memory;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 版对话记忆仓库（实现 Spring AI ChatMemoryRepository，1.1.7 无官方 Redis 实现）
 *
 * 设计要点：
 * - conversationId 格式为 "userId:sessionId"，与 MySQL 的 (user_id, session_id) 二元组对应；
 * - MessageWindowChatMemory 的 saveAll 是"整窗替换"语义，故写入采用整体覆盖 set；
 * - MySQL ai_conversation 表仍由 AgentServiceImpl 追加完整历史（供前端会话列表/历史展示），
 *   Redis 仅存供模型消费的滑动窗口记忆，职责分离；
 * - TTL 由配置控制（默认 7 天），每次写入刷新。
 */
@Service
public class ConversationMemoryService implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${agent.memory.redis-prefix:agent:memory:}")
    private String redisPrefix;

    @Value("${agent.memory.ttl-days:7}")
    private int ttlDays;

    public ConversationMemoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(redisPrefix + "*");
        List<String> ids = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                ids.add(key.substring(redisPrefix.length()));
            }
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = redisTemplate.opsForValue().get(key(conversationId));
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            List<Message> messages = new ArrayList<>(array.size());
            for (Object element : array) {
                JSONObject stored = (JSONObject) element;
                String type = stored.getStr("type");
                String text = stored.getStr("text");
                switch (type) {
                    case "USER" -> messages.add(new UserMessage(text));
                    case "ASSISTANT" -> messages.add(new AssistantMessage(text));
                    case "SYSTEM" -> messages.add(new SystemMessage(text));
                    // TOOL 消息为框架内部协议细节，不参与回放
                    default -> log.debug("[Memory] 跳过不支持的回放消息类型: {}", type);
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("[Memory] 会话 {} 记忆反序列化失败，按空处理: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 整窗替换语义：窗口清空即删除 key
        if (messages == null || messages.isEmpty()) {
            deleteByConversationId(conversationId);
            return;
        }
        JSONArray array = new JSONArray();
        for (Message message : messages) {
            array.add(new JSONObject()
                    .set("type", message.getMessageType().name())
                    .set("text", message.getText()));
        }
        redisTemplate.opsForValue().set(key(conversationId), array.toString(),
                Duration.ofDays(ttlDays));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(key(conversationId));
    }

    private String key(String conversationId) {
        return redisPrefix + conversationId;
    }
}
