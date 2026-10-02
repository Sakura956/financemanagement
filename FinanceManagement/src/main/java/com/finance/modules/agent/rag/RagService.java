package com.finance.modules.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.bill.entity.BillCategory;
import com.finance.modules.bill.entity.BillRecord;
import com.finance.modules.bill.mapper.BillCategoryMapper;
import com.finance.modules.bill.mapper.BillRecordMapper;
import com.finance.modules.memo.entity.Memo;
import com.finance.modules.memo.mapper.MemoMapper;
import com.finance.modules.plan.entity.FinancePlan;
import com.finance.modules.plan.mapper.FinancePlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RAG 核心服务：向量索引管理 + 相似度检索
 *
 * 向量库为内存版 SimpleVectorStore（重启丢失），因此：
 * - 启动时由 VectorIndexRunner 全量重建所有用户索引；
 * - Agent 工具写入数据时（记一笔/建计划/写备忘）做增量 indexXxx / removeXxx；
 * - 提供手动全量重建入口（调试接口）。
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStore vectorStore;
    private final ChunkingService chunkingService;
    private final BillRecordMapper billRecordMapper;
    private final BillCategoryMapper billCategoryMapper;
    private final FinancePlanMapper financePlanMapper;
    private final MemoMapper memoMapper;

    @Value("${agent.rag.enabled:true}")
    private boolean enabled;

    @Value("${agent.rag.top-k:5}")
    private int topK;

    @Value("${agent.rag.similarity-threshold:0.2}")
    private double similarityThreshold;

    /** docId 登记簿：userId → 该用户全部文档ID（全量重建时先删后加） */
    private final Map<Long, Set<String>> userDocIds = new ConcurrentHashMap<>();

    /** 实体键（如 bill:123）→ 文档ID（单实体增量删除用） */
    private final Map<String, String> entityDocIds = new ConcurrentHashMap<>();

    public RagService(VectorStore vectorStore,
                      ChunkingService chunkingService,
                      BillRecordMapper billRecordMapper,
                      BillCategoryMapper billCategoryMapper,
                      FinancePlanMapper financePlanMapper,
                      MemoMapper memoMapper) {
        this.vectorStore = vectorStore;
        this.chunkingService = chunkingService;
        this.billRecordMapper = billRecordMapper;
        this.billCategoryMapper = billCategoryMapper;
        this.financePlanMapper = financePlanMapper;
        this.memoMapper = memoMapper;
    }

    /**
     * 全量重建用户向量索引（删除旧文档 → 重新切片 → 写入向量库）
     */
    public synchronized int rebuildUserIndex(Long userId) {
        if (!enabled) return 0;

        // 1. 删除该用户的旧文档
        deleteByEntityPrefix(userId);

        // 2. 加载三类业务数据
        List<BillRecord> bills = billRecordMapper.selectList(
                new LambdaQueryWrapper<BillRecord>().eq(BillRecord::getUserId, userId));
        List<FinancePlan> plans = financePlanMapper.selectList(
                new LambdaQueryWrapper<FinancePlan>().eq(FinancePlan::getUserId, userId));
        List<Memo> memos = memoMapper.selectList(
                new LambdaQueryWrapper<Memo>().eq(Memo::getUserId, userId));

        // 3. 分类名称映射（账单文本需要）
        Map<Long, String> categoryNames = bills.isEmpty() ? Map.of()
                : billCategoryMapper.selectBatchIds(bills.stream()
                        .map(BillRecord::getCategoryId)
                        .filter(java.util.Objects::nonNull)
                        .distinct().toList())
                .stream().collect(Collectors.toMap(BillCategory::getId, BillCategory::getName));

        // 4. 切片并写入
        List<Document> docs = new ArrayList<>();
        docs.addAll(chunkingService.chunkBills(bills, categoryNames));
        docs.addAll(chunkingService.chunkPlans(plans));
        docs.addAll(chunkingService.chunkMemos(memos));

        int count = 0;
        for (Document doc : docs) {
            addDocument(userId, entityKey((String) doc.getMetadata().get("type"),
                    doc.getMetadata().get("refId")), doc);
            count++;
        }

        log.info("[RAG] Indexed {} documents for user {} (bills={}, plans={}, memos={})",
                count, userId, bills.size(), plans.size(), memos.size());
        return count;
    }

    /**
     * 相似度检索：按 userId 过滤 + 向量相似度搜索
     *
     * @return 相关文档列表（可能为空）
     */
    public List<Document> search(Long userId, String question) {
        if (!enabled || !StringUtils.hasText(question)) {
            return List.of();
        }
        try {
            return vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(new FilterExpressionBuilder().eq("userId", userId).build())
                    .build());
        } catch (Exception e) {
            // Embedding API 异常不应阻断对话（Agent 还有工具兜底）
            log.warn("[RAG] 检索失败，降级为空上下文: {}", e.getMessage());
            return List.of();
        }
    }

    // ===== 增量索引（Agent 工具写入数据后调用） =====

    public void indexBill(BillRecord bill, String categoryName) {
        if (!enabled || bill == null) return;
        try {
            addDocument(bill.getUserId(), entityKey("bill", bill.getId()),
                    chunkingService.chunkBill(bill, categoryName));
        } catch (Exception e) {
            // 业务写入已成功，索引失败只影响检索召回，不应让工具报错
            log.warn("[RAG] 账单 {} 增量索引失败（不影响业务写入）: {}", bill.getId(), e.getMessage());
        }
    }

    public void indexPlan(FinancePlan plan) {
        if (!enabled || plan == null) return;
        try {
            addDocument(plan.getUserId(), entityKey("plan", plan.getId()), chunkingService.chunkPlan(plan));
        } catch (Exception e) {
            log.warn("[RAG] 计划 {} 增量索引失败（不影响业务写入）: {}", plan.getId(), e.getMessage());
        }
    }

    public void indexMemo(Memo memo) {
        if (!enabled || memo == null) return;
        try {
            addDocument(memo.getUserId(), entityKey("memo", memo.getId()), chunkingService.chunkMemo(memo));
        } catch (Exception e) {
            log.warn("[RAG] 备忘 {} 增量索引失败（不影响业务写入）: {}", memo.getId(), e.getMessage());
        }
    }

    public void removeBill(Long billId) {
        removeEntity("bill", billId);
    }

    // ===== 统计信息（健康检查 / 调试用） =====

    /** 当前用户已索引文档数 */
    public int countUserDocuments(Long userId) {
        Set<String> ids = userDocIds.get(userId);
        return ids != null ? ids.size() : 0;
    }

    /** 向量库总文档数 */
    public int totalDocuments() {
        return userDocIds.values().stream().mapToInt(Set::size).sum();
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===== 内部方法 =====

    private void addDocument(Long userId, String entityKey, Document doc) {
        vectorStore.add(List.of(doc));
        userDocIds.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(doc.getId());
        entityDocIds.put(entityKey, doc.getId());
    }

    private void removeEntity(String type, Long refId) {
        String docId = entityDocIds.remove(entityKey(type, refId));
        if (docId != null) {
            vectorStore.delete(List.of(docId));
            userDocIds.values().forEach(ids -> ids.remove(docId));
        }
    }

    /** 删除某用户全部旧文档（全量重建第一步） */
    private void deleteByEntityPrefix(Long userId) {
        Set<String> ids = userDocIds.remove(userId);
        if (ids != null && !ids.isEmpty()) {
            vectorStore.delete(new ArrayList<>(ids));
        }
        // entityDocIds 的旧映射会被随后的 addDocument 覆盖，无需额外清理
    }

    private String entityKey(String type, Object refId) {
        return type + ":" + refId;
    }
}
