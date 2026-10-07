package com.finance.modules.report.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 报告模块配置
 *
 * reportChatClient：专用于周报生成的"干净" ChatClient——
 * 不挂工具回调、不挂对话记忆（报告是一次性生成任务，
 * 避免 LLM 在生成过程中误调工具或写入会话记忆）。
 */
@Configuration
public class ReportConfig {

    @Bean
    public ChatClient reportChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
