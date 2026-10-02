package com.finance.modules.agent.controller;

import com.finance.common.result.Result;
import com.finance.modules.agent.dto.AgentChatRequest;
import com.finance.modules.agent.dto.AgentChatResponse;
import com.finance.modules.agent.dto.AgentSseEvent;
import com.finance.modules.agent.service.AgentService;
import com.finance.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Agent 对话控制器
 *
 * SSE 说明：流式接口直接下发 AgentSseEvent（JSON），
 * 事件序列 session → tool* / content*（交错）→ sources → done。
 */
@RestController
@RequestMapping("/api/v1/user/agent")
public class AgentChatController {

    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 非流式对话
     */
    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(agentService.chat(userId, request));
    }

    /**
     * 流式对话（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentSseEvent> chatStream(@Valid @RequestBody AgentChatRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return agentService.chatStream(userId, request);
    }

    /**
     * 删除会话（MySQL 历史 + Redis 对话记忆）
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        agentService.clearSession(userId, sessionId);
        return Result.success("会话已删除", null);
    }
}
