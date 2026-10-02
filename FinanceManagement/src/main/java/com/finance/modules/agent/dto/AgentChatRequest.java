package com.finance.modules.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 对话请求 DTO
 */
@Data
public class AgentChatRequest {

    /** 会话ID（继续多轮对话时传入，新对话不传由后端生成） */
    private String sessionId;

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息最多2000字")
    private String message;
}
