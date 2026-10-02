package com.finance.modules.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * Agent SSE 流式事件
 * 事件序列：session → tool* / content*（交错）→ sources → done（异常时 → error → done）
 */
@Data
public class AgentSseEvent {

    /** 事件类型：session-会话信息 / tool-工具调用 / content-正文增量 / sources-参考来源 / done-结束 / error-错误 */
    private String type;

    /** 会话ID */
    private String sessionId;

    /** content 事件的正文增量 */
    private String text;

    /** tool 事件的工具调用详情 */
    private ToolCallEvent toolCall;

    /** sources 事件的参考来源列表 */
    private List<RagSource> sources;

    /** error 事件的错误信息 */
    private String message;

    public static AgentSseEvent of(String type) {
        AgentSseEvent event = new AgentSseEvent();
        event.setType(type);
        return event;
    }
}
