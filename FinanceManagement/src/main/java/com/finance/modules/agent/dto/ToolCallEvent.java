package com.finance.modules.agent.dto;

import lombok.Data;

/**
 * 工具调用事件（用于前端可视化：展示 AI 正在"查账单""记一笔"等动作）
 */
@Data
public class ToolCallEvent {

    /** 关联的会话ID */
    private String sessionId;

    /** 工具方法名，如 queryBills */
    private String toolName;

    /** 工具展示名，如"查询账单" */
    private String toolLabel;

    /** 状态：start-开始执行 / success-执行成功 / error-执行失败 */
    private String status;

    /** AI 传入的参数 JSON（截断展示用） */
    private String arguments;

    /** 工具返回结果摘要（截断展示用） */
    private String result;

    /** 事件时间戳（毫秒） */
    private long timestamp;
}
