package com.finance.modules.agent.tools;

import com.finance.modules.agent.dto.ToolCallEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调用事件发布器
 *
 * 每次对话开始时注册一个会话级 Sink，AI 执行工具时实时推送事件，
 * SSE 接口把这些事件与正文增量合并下发，前端据此渲染"工具调用徽章"。
 */
@Component
public class AgentToolEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgentToolEventPublisher.class);

    /** 会话ID → 事件流 */
    private final Map<String, Sinks.Many<ToolCallEvent>> sinks = new ConcurrentHashMap<>();

    /** 工具名 → 中文展示名（前端徽章文案） */
    private static final Map<String, String> TOOL_LABELS = Map.ofEntries(
            Map.entry("queryBills", "查询账单"),
            Map.entry("createBill", "记一笔账单"),
            Map.entry("deleteBill", "删除账单"),
            Map.entry("getMonthlyOverview", "统计月度概览"),
            Map.entry("getCategoryDistribution", "统计分类占比"),
            Map.entry("getTrend", "统计收支趋势"),
            Map.entry("listPlans", "查询财务计划"),
            Map.entry("createPlan", "创建财务计划"),
            Map.entry("getPlanValuation", "查询计划估值"),
            Map.entry("listMemos", "查询备忘录"),
            Map.entry("createMemo", "创建备忘录")
    );

    /**
     * 为会话注册事件流（对话开始时调用）
     */
    public void register(String sessionId) {
        sinks.put(sessionId, Sinks.many().unicast().onBackpressureBuffer());
    }

    /**
     * 获取会话事件流（未注册时返回空流）
     */
    public Flux<ToolCallEvent> flux(String sessionId) {
        Sinks.Many<ToolCallEvent> sink = sinks.get(sessionId);
        return sink != null ? sink.asFlux() : Flux.empty();
    }

    /**
     * 发布工具调用事件
     */
    public void emit(ToolCallEvent event) {
        Sinks.Many<ToolCallEvent> sink = event.getSessionId() != null ? sinks.get(event.getSessionId()) : null;
        if (sink != null) {
            sink.tryEmitNext(event);
        }
        // 同步记录日志，便于后端观察 Agent 的工具调用行为
        log.info("[Agent] 工具调用 {} [{}] args={} result={}",
                event.getToolName(), event.getStatus(), event.getArguments(), event.getResult());
    }

    /**
     * 结束会话事件流（对话结束时调用，无论成功失败）
     */
    public void complete(String sessionId) {
        Sinks.Many<ToolCallEvent> sink = sessionId != null ? sinks.remove(sessionId) : null;
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    /**
     * 获取工具中文展示名
     */
    public static String labelFor(String toolName) {
        return TOOL_LABELS.getOrDefault(toolName, toolName);
    }
}
