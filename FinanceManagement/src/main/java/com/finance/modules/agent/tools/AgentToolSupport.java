package com.finance.modules.agent.tools;

import com.finance.util.SecurityUtil;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Agent 工具层公共支撑方法
 *
 * 关键设计：流式（SSE）模式下，工具方法由框架在 Reactor 线程上执行，
 * SecurityContextHolder（ThreadLocal）中已经没有登录信息，
 * 因此 userId 统一通过 ToolContext 传递（由 AgentServiceImpl 在每次请求时注入），
 * 绝不信任 AI 传参，保证用户数据隔离。
 */
public final class AgentToolSupport {

    private AgentToolSupport() {
    }

    /** ToolContext 中存放用户ID的 Key */
    public static final String CTX_USER_ID = "userId";

    /** ToolContext 中存放会话ID的 Key */
    public static final String CTX_SESSION_ID = "sessionId";

    /**
     * 从 ToolContext 中获取当前用户ID；
     * 取不到时（如非流式同线程调用）降级从 SecurityContext 获取
     */
    public static Long currentUserId(ToolContext toolContext) {
        if (toolContext != null) {
            Object userId = toolContext.getContext().get(CTX_USER_ID);
            if (userId instanceof Long id) {
                return id;
            }
            if (userId instanceof Number n) {
                return n.longValue();
            }
            if (userId instanceof String s && !s.isBlank()) {
                return Long.parseLong(s);
            }
        }
        // 降级：同线程调用时直接从安全上下文取（非流式模式）
        return SecurityUtil.getCurrentUserId();
    }

    /**
     * 从 ToolContext 中获取会话ID（可能为 null）
     */
    public static String currentSessionId(ToolContext toolContext) {
        if (toolContext != null) {
            Object sessionId = toolContext.getContext().get(CTX_SESSION_ID);
            if (sessionId != null) {
                return String.valueOf(sessionId);
            }
        }
        return null;
    }

    /**
     * 截断字符串（工具结果摘要展示用），并移除换行避免事件过长
     */
    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        String flat = text.replace("\n", " ").replace("\r", " ").trim();
        return flat.length() <= maxLen ? flat : flat.substring(0, maxLen) + "...";
    }
}
