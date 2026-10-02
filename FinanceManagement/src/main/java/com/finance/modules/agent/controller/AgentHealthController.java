package com.finance.modules.agent.controller;

import com.finance.common.result.Result;
import com.finance.modules.agent.rag.RagService;
import com.finance.util.SecurityUtil;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 健康检查与调试控制器
 *
 * 输出 Agent 各组件（模型/工具/RAG）状态，并提供当前用户向量索引手动重建入口。
 */
@RestController
@RequestMapping("/api/v1/user/agent")
public class AgentHealthController {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final RagService ragService;
    private final List<ToolCallback> agentToolCallbacks;

    public AgentHealthController(ChatModel chatModel,
                                 EmbeddingModel embeddingModel,
                                 RagService ragService,
                                 List<ToolCallback> agentToolCallbacks) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.ragService = ragService;
        this.agentToolCallbacks = agentToolCallbacks;
    }

    /**
     * Agent 组件状态总览
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        SecurityUtil.getCurrentUserId(); // 需登录才可探测

        Map<String, Object> rag = new LinkedHashMap<>();
        rag.put("enabled", ragService.isEnabled());
        rag.put("totalDocuments", ragService.totalDocuments());

        List<String> toolNames = agentToolCallbacks.stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("chatModel", chatModel.getClass().getSimpleName());
        status.put("embeddingModel", embeddingModel.getClass().getSimpleName());
        status.put("tools", Map.of("count", toolNames.size(), "names", toolNames));
        status.put("rag", rag);
        return Result.success(status);
    }

    /**
     * 手动重建当前用户的向量索引（调试用）
     */
    @PostMapping("/index/rebuild")
    public Result<Map<String, Object>> rebuildIndex() {
        Long userId = SecurityUtil.getCurrentUserId();
        int documents = ragService.rebuildUserIndex(userId);
        return Result.success(Map.of(
                "userId", userId,
                "documents", documents
        ));
    }
}
