package com.finance.modules.agent.tools;

import com.finance.modules.agent.dto.ToolCallEvent;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * 可观测的工具回调装饰器
 *
 * 包装真正的 ToolCallback，在执行前后向 AgentToolEventPublisher 发布事件，
 * 实现"AI 正在做什么"的前端可视化（工具调用徽章）。
 */
public class ObservableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final AgentToolEventPublisher publisher;

    public ObservableToolCallback(ToolCallback delegate, AgentToolEventPublisher publisher) {
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public String call(String toolInput) {
        return observe(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return observe(toolInput, toolContext);
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private String observe(String toolInput, ToolContext toolContext) {
        String sessionId = AgentToolSupport.currentSessionId(toolContext);
        String toolName = delegate.getToolDefinition().name();

        publisher.emit(event(sessionId, toolName, "start", toolInput, null));

        try {
            String result = toolContext == null
                    ? delegate.call(toolInput)
                    : delegate.call(toolInput, toolContext);
            publisher.emit(event(sessionId, toolName, "success", toolInput, result));
            return result;
        } catch (Exception e) {
            publisher.emit(event(sessionId, toolName, "error", toolInput, e.getMessage()));
            throw e;
        }
    }

    private ToolCallEvent event(String sessionId, String toolName, String status,
                               String arguments, String result) {
        ToolCallEvent event = new ToolCallEvent();
        event.setSessionId(sessionId);
        event.setToolName(toolName);
        event.setToolLabel(AgentToolEventPublisher.labelFor(toolName));
        event.setStatus(status);
        event.setArguments(AgentToolSupport.truncate(arguments, 200));
        event.setResult(AgentToolSupport.truncate(result, 200));
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
}
