package com.finance.modules.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * Agent 对话响应 DTO（非流式接口）
 */
@Data
public class AgentChatResponse {

    /** 会话ID */
    private String sessionId;

    /** AI 最终回复内容 */
    private String message;

    /** 本次对话 AI 调用过的工具（可视化用） */
    private List<ToolCallEvent> toolCalls;

    /** RAG 检索到的参考来源（溯源用） */
    private List<RagSource> sources;

    /** 创建时间 yyyy-MM-dd HH:mm:ss */
    private String createdAt;
}
