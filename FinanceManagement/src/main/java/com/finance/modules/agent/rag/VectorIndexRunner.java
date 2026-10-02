package com.finance.modules.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.auth.entity.User;
import com.finance.modules.auth.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量索引启动重建器
 *
 * SimpleVectorStore 为内存实现，应用重启后数据丢失。
 * 本类在应用启动完成后开启后台线程，为所有用户全量重建 RAG 索引，
 * 避免阻塞启动流程；用户间加入短暂间隔，防止 Embedding API 限流。
 */
@Component
public class VectorIndexRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexRunner.class);

    /** 用户间重建间隔（毫秒），避免 Embedding API 限流 */
    private static final long INTER_USER_DELAY_MS = 200L;

    private final RagService ragService;
    private final UserMapper userMapper;

    @Value("${agent.rag.index-on-startup:true}")
    private boolean indexOnStartup;

    public VectorIndexRunner(RagService ragService, UserMapper userMapper) {
        this.ragService = ragService;
        this.userMapper = userMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!indexOnStartup || !ragService.isEnabled()) {
            log.info("[RAG] 启动索引已跳过 (index-on-startup={}, rag.enabled={})",
                    indexOnStartup, ragService.isEnabled());
            return;
        }

        Thread worker = new Thread(this::rebuildAllUsers, "rag-index-runner");
        worker.setDaemon(true);
        worker.start();
    }

    private void rebuildAllUsers() {
        try {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().select(User::getId));
            log.info("[RAG] 启动全量索引重建开始，用户数: {}", users.size());

            int totalDocs = 0;
            for (User user : users) {
                try {
                    totalDocs += ragService.rebuildUserIndex(user.getId());
                } catch (Exception e) {
                    log.warn("[RAG] 用户 {} 索引重建失败: {}", user.getId(), e.getMessage());
                }
                try {
                    Thread.sleep(INTER_USER_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[RAG] 启动索引重建被中断");
                    return;
                }
            }
            log.info("[RAG] 启动全量索引重建完成，共索引 {} 篇文档", totalDocs);
        } catch (Exception e) {
            log.error("[RAG] 启动索引重建异常: {}", e.getMessage(), e);
        }
    }
}
