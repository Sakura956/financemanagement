package com.finance.modules.agent.service.impl;

import com.finance.modules.agent.dto.AgentChatRequest;
import com.finance.modules.agent.dto.AgentChatResponse;
import com.finance.modules.agent.dto.AgentSseEvent;
import com.finance.modules.agent.dto.RagSource;
import com.finance.modules.agent.dto.ToolCallEvent;
import com.finance.modules.agent.rag.RagService;
import com.finance.modules.agent.service.AgentService;
import com.finance.modules.agent.tools.AgentToolEventPublisher;
import com.finance.modules.agent.tools.AgentToolSupport;
import com.finance.modules.ai.entity.AiConversation;
import com.finance.modules.ai.mapper.AiConversationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 编排服务实现
 *
 * 流程：RAG 检索 → 拼 System Prompt → ChatClient（工具 + 记忆 Advisor）→
 * SSE 事件流（session → tool 与 content 交错 → sources → done）→ MySQL 历史落库。
 *
 * 注意：
 * - userId 显式传参并注入 ToolContext，避免工具在 Reactor 线程执行时
 *   SecurityContext ThreadLocal 丢失导致的 401；
 * - Redis 记忆由 MessageChatMemoryAdvisor 维护（窗口替换语义），
 *   MySQL 历史由本类追加写入（供前端会话列表/历史展示），职责分离。
 */
@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 非流式接口等待流式完成的超时时间 */
    private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(120);

    private static final String BASE_SYSTEM_PROMPT = """
            你是"FinanceAgent"，一个专业的个人财务管理智能助手。

            ## 你的能力
            你可以通过调用工具直接操作当前用户的财务数据：
            - 账单管理：查询、记录、删除收支账单
            - 统计分析：月度收支概览、分类占比、收支趋势
            - 财务计划：创建与查询理财计划及其持仓估值
            - 备忘录：创建与查询备忘事项

            ## 行为准则
            1. 涉及用户真实数据的问题（如"我这个月花了多少"），必须调用工具查询，严禁编造数字；
            2. 用户要求记录/创建数据时，关键信息（金额、分类、日期）缺失则合理追问；若用户未提供日期，默认使用今天；
            3. 金额统一用元，保留两位小数；
            4. 回答使用简体中文，语气友好、专业、简洁，适当使用列表让信息更清晰；
            5. 提供理财建议时客观中立并提示风险，声明不构成投资建议。""";

    private final ChatClient agentChatClient;
    private final RagService ragService;
    private final AgentToolEventPublisher toolEventPublisher;
    private final AiConversationMapper conversationMapper;
    private final com.finance.modules.agent.memory.ConversationMemoryService memoryService;

    public AgentServiceImpl(ChatClient agentChatClient,
                            RagService ragService,
                            AgentToolEventPublisher toolEventPublisher,
                            AiConversationMapper conversationMapper,
                            com.finance.modules.agent.memory.ConversationMemoryService memoryService) {
        this.agentChatClient = agentChatClient;
        this.ragService = ragService;
        this.toolEventPublisher = toolEventPublisher;
        this.conversationMapper = conversationMapper;
        this.memoryService = memoryService;
    }

    // ===== 非流式（内部订阅流式编排，逻辑单份维护） =====

    @Override
    public AgentChatResponse chat(Long userId, AgentChatRequest request) {
        List<AgentSseEvent> events = chatStream(userId, request)
                .collectList()
                .block(CHAT_TIMEOUT);

        AgentChatResponse response = new AgentChatResponse();
        response.setToolCalls(new ArrayList<>());
        StringBuilder message = new StringBuilder();

        if (events != null) {
            for (AgentSseEvent event : events) {
                switch (event.getType()) {
                    case "session" -> response.setSessionId(event.getSessionId());
                    case "content" -> message.append(event.getText());
                    case "tool" -> response.getToolCalls().add(event.getToolCall());
                    case "sources" -> response.setSources(event.getSources());
                    case "error" -> {
                        log.warn("[Agent] 非流式对话出错: {}", event.getMessage());
                    }
                    default -> { /* done 忽略 */ }
                }
            }
        }

        response.setMessage(message.toString());
        response.setCreatedAt(LocalDateTime.now().format(DATE_TIME));
        return response;
    }

    // ===== 流式（SSE） =====

    @Override
    public Flux<AgentSseEvent> chatStream(Long userId, AgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String conversationId = userId + ":" + sessionId;

        // 1. 用户消息先行落库（前端会话列表/历史沿用旧接口读取 MySQL）
        saveUserMessage(userId, sessionId, request.getMessage());

        // 2. 注册工具事件流（AI 调工具时实时发射，供 SSE 合并下发）
        toolEventPublisher.register(sessionId);

        // 3. RAG 检索 → 参考来源 + 知识库上下文
        List<Document> documents = ragService.search(userId, request.getMessage());
        List<RagSource> sources = documents.stream().map(this::toRagSource).toList();
        String systemPrompt = buildSystemPrompt(documents);

        // 4. 收集器：完整回复 + 工具调用记录
        StringBuilder fullReply = new StringBuilder();
        List<ToolCallEvent> toolCalls = new CopyOnWriteArrayList<>();

        // 5. 正文增量流（流结束时释放工具事件 Sink）
        Flux<AgentSseEvent> contentFlux = agentChatClient.prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(
                        AgentToolSupport.CTX_USER_ID, userId,
                        AgentToolSupport.CTX_SESSION_ID, sessionId))
                .stream()
                .content()
                .doOnNext(fullReply::append)
                .map(text -> {
                    AgentSseEvent event = AgentSseEvent.of("content");
                    event.setSessionId(sessionId);
                    event.setText(text);
                    return event;
                })
                .doFinally(signal -> toolEventPublisher.complete(sessionId));

        // 6. 工具调用事件流（与正文交错合并）
        Flux<AgentSseEvent> toolFlux = toolEventPublisher.flux(sessionId)
                .doOnNext(toolCalls::add)
                .map(toolEvent -> {
                    AgentSseEvent event = AgentSseEvent.of("tool");
                    event.setSessionId(sessionId);
                    event.setToolCall(toolEvent);
                    return event;
                });

        // 7. 编排完整事件序列：session → (tool* / content*) → sources → done
        return Flux.concat(
                        Flux.just(sessionEvent(sessionId)),
                        Flux.merge(toolFlux, contentFlux)
                                .concatWith(Flux.defer(() -> {
                                    // 流正常完成：AI 回复落库，随后下发来源与结束事件
                                    saveAssistantMessage(userId, sessionId, fullReply.toString());
                                    return Flux.just(sourcesEvent(sessionId, sources), doneEvent(sessionId));
                                })))
                .onErrorResume(e -> {
                    log.error("[Agent] 会话 {} 流式对话异常: {}", sessionId, e.getMessage(), e);
                    toolEventPublisher.complete(sessionId); // 兜底释放 Sink
                    AgentSseEvent error = AgentSseEvent.of("error");
                    error.setSessionId(sessionId);
                    error.setMessage("AI 服务暂时不可用，请稍后重试");
                    return Flux.just(error, doneEvent(sessionId));
                });
    }

    // ===== 会话清理 =====

    @Override
    public void clearSession(Long userId, String sessionId) {
        conversationMapper.deleteBySessionId(userId, sessionId);
        memoryService.deleteByConversationId(userId + ":" + sessionId);
        log.info("[Agent] 会话已清理: userId={}, sessionId={}", userId, sessionId);
    }

    // ===== 内部方法 =====

    private String buildSystemPrompt(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return BASE_SYSTEM_PROMPT;
        }
        StringBuilder prompt = new StringBuilder(BASE_SYSTEM_PROMPT)
                .append("\n\n## 参考资料（检索自用户个人知识库，可能不完整，实时数据请调用工具查询）\n");
        int index = 1;
        for (Document doc : documents) {
            prompt.append("[").append(index++).append("] ")
                    .append(doc.getText().replace("\n", " "))
                    .append("\n");
        }
        return prompt.toString();
    }

    private RagSource toRagSource(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        RagSource source = new RagSource();
        Object type = meta.get("type");
        Object refId = meta.get("refId");
        source.setType(type != null ? type.toString() : null);
        if (refId instanceof Number number) {
            source.setRefId(number.longValue());
        }
        source.setTitle((String) meta.get("title"));
        source.setDetail((String) meta.get("detail"));
        return source;
    }

    private AgentSseEvent sessionEvent(String sessionId) {
        AgentSseEvent event = AgentSseEvent.of("session");
        event.setSessionId(sessionId);
        return event;
    }

    private AgentSseEvent sourcesEvent(String sessionId, List<RagSource> sources) {
        AgentSseEvent event = AgentSseEvent.of("sources");
        event.setSessionId(sessionId);
        event.setSources(sources);
        return event;
    }

    private AgentSseEvent doneEvent(String sessionId) {
        AgentSseEvent event = AgentSseEvent.of("done");
        event.setSessionId(sessionId);
        return event;
    }

    private void saveUserMessage(Long userId, String sessionId, String content) {
        try {
            AiConversation message = new AiConversation();
            message.setUserId(userId);
            message.setSessionId(sessionId);
            message.setRole("user");
            message.setContent(content);
            message.setTokensUsed(0);
            message.setCreateTime(LocalDateTime.now());
            conversationMapper.insert(message);
        } catch (Exception e) {
            log.warn("[Agent] 用户消息落库失败: sessionId={}, {}", sessionId, e.getMessage());
        }
    }

    private void saveAssistantMessage(Long userId, String sessionId, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        try {
            AiConversation message = new AiConversation();
            message.setUserId(userId);
            message.setSessionId(sessionId);
            message.setRole("assistant");
            message.setContent(content);
            message.setTokensUsed(content.length());
            message.setCreateTime(LocalDateTime.now());
            conversationMapper.insert(message);
        } catch (Exception e) {
            log.warn("[Agent] AI 消息落库失败: sessionId={}, {}", sessionId, e.getMessage());
        }
    }
}
