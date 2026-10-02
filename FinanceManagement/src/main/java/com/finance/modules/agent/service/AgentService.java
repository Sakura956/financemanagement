package com.finance.modules.agent.service;

import com.finance.modules.agent.dto.AgentChatRequest;
import com.finance.modules.agent.dto.AgentChatResponse;
import com.finance.modules.agent.dto.AgentSseEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 编排服务：ChatClient + 工具 + RAG + 对话记忆
 */
public interface AgentService {

    /**
     * 非流式对话（内部复用流式编排，一次性返回完整结果）
     */
    AgentChatResponse chat(Long userId, AgentChatRequest request);

    /**
     * 流式对话（SSE）
     * 事件序列：session → tool* / content*（交错）→ sources → done（异常时 error → done）
     */
    Flux<AgentSseEvent> chatStream(Long userId, AgentChatRequest request);

    /**
     * 删除会话：MySQL 历史 + Redis 对话记忆
     */
    void clearSession(Long userId, String sessionId);
}
