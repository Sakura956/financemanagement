package com.finance.modules.agent.config;

import com.finance.modules.agent.memory.ConversationMemoryService;
import com.finance.modules.agent.tools.AgentToolEventPublisher;
import com.finance.modules.agent.tools.BillTools;
import com.finance.modules.agent.tools.MemoTools;
import com.finance.modules.agent.tools.ObservableToolCallback;
import com.finance.modules.agent.tools.PlanTools;
import com.finance.modules.agent.tools.StatisticsTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 模块配置：向量库 / 对话记忆 / 工具回调 / ChatClient
 *
 * 说明：
 * - SimpleVectorStore 为内存实现（无对应 starter，手动声明 Bean），重启由 VectorIndexRunner 重建；
 * - 对话记忆 = Redis 仓库（ConversationMemoryService）+ 滑动窗口（MessageWindowChatMemory）；
 * - 工具回调经 ObservableToolCallback 装饰，执行过程实时推送前端可视化事件；
 * - MessageChatMemoryAdvisor 以 conversationId（"userId:sessionId"）区分多会话记忆。
 */
@Configuration
public class AgentConfig {

    @Value("${agent.memory.max-messages:20}")
    private int maxMessages;

    /**
     * 内存向量库（基于 EmbeddingModel 构建，生产可替换为 Milvus/PGVector 等）
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 对话记忆：Redis 仓库 + 默认 20 条滑动窗口
     */
    @Bean
    public ChatMemory agentChatMemory(ConversationMemoryService repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * Agent 工具回调：4 个工具类共 11 个 @Tool 方法，统一装饰为可观测回调
     */
    @Bean
    public List<ToolCallback> agentToolCallbacks(BillTools billTools,
                                                 StatisticsTools statisticsTools,
                                                 PlanTools planTools,
                                                 MemoTools memoTools,
                                                 AgentToolEventPublisher publisher) {
        ToolCallback[] raw = MethodToolCallbackProvider.builder()
                .toolObjects(billTools, statisticsTools, planTools, memoTools)
                .build()
                .getToolCallbacks();

        List<ToolCallback> observed = new ArrayList<>(raw.length);
        for (ToolCallback callback : raw) {
            observed.add(new ObservableToolCallback(callback, publisher));
        }
        return observed;
    }

    /**
     * Agent 专用 ChatClient：默认注册全部工具 + 对话记忆 Advisor
     */
    @Bean
    public ChatClient agentChatClient(ChatModel chatModel,
                                      ChatMemory agentChatMemory,
                                      List<ToolCallback> agentToolCallbacks) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(agentToolCallbacks.toArray(new ToolCallback[0]))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(agentChatMemory).build())
                .build();
    }
}
