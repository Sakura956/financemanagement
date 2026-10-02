package com.finance.modules.agent.rag;

import com.finance.modules.bill.entity.BillRecord;
import com.finance.modules.memo.entity.Memo;
import com.finance.modules.plan.entity.FinancePlan;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务实体切片服务
 *
 * 切片策略（按业务实体切片）：每条账单/计划/备忘 = 1 个 Document
 * Embedding 输入为"类型 | 关键字段"的结构化文本；
 * metadata 中带 userId 用于检索时做用户隔离过滤，带 refId/title/detail 用于前端溯源展示。
 * 长备忘（>500字）按段落 + 滑动窗口二次切分。
 */
@Service
public class ChunkingService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 长文本二次切分：单块最大字符数 */
    private static final int CHUNK_SIZE = 400;
    /** 滑动窗口重叠字符数（保留上下文） */
    private static final int CHUNK_OVERLAP = 100;
    /** 超过该长度触发二次切分 */
    private static final int SPLIT_THRESHOLD = 500;
    /** 溯源卡片 detail 截断长度 */
    private static final int DETAIL_MAX = 80;

    // ===== 账单 =====

    public List<Document> chunkBills(List<BillRecord> bills, Map<Long, String> categoryNames) {
        List<Document> docs = new ArrayList<>();
        for (BillRecord bill : bills) {
            docs.add(chunkBill(bill, categoryNames.getOrDefault(bill.getCategoryId(), "未知")));
        }
        return docs;
    }

    public Document chunkBill(BillRecord bill, String categoryName) {
        String content = String.format(
                "账单记录 | 日期: %s | 类型: %s | 分类: %s | 金额: %.2f元\n备注: %s",
                bill.getRecordTime() != null ? bill.getRecordTime().format(DATETIME) : "未知",
                bill.getType() != null && bill.getType() == 1 ? "收入" : "支出",
                categoryName,
                bill.getAmount() != null ? bill.getAmount() : 0,
                bill.getDescription() != null ? bill.getDescription() : "");

        Map<String, Object> metadata = baseMetadata("bill", bill.getId(), bill.getUserId());
        metadata.put("category", categoryName);
        metadata.put("date", bill.getRecordTime() != null ? bill.getRecordTime().format(DATE) : null);
        // 溯源展示
        metadata.put("title", String.format("%s %s%s %.2f元",
                bill.getRecordTime() != null ? bill.getRecordTime().format(DATE) : "",
                categoryName, bill.getType() != null && bill.getType() == 1 ? "收入" : "支出",
                bill.getAmount() != null ? bill.getAmount() : 0));
        metadata.put("detail", truncate(bill.getDescription(), DETAIL_MAX));
        return new Document(content, metadata);
    }

    // ===== 财务计划 =====

    public List<Document> chunkPlans(List<FinancePlan> plans) {
        List<Document> docs = new ArrayList<>();
        for (FinancePlan plan : plans) {
            docs.add(chunkPlan(plan));
        }
        return docs;
    }

    public Document chunkPlan(FinancePlan plan) {
        String content = String.format(
                "财务计划 | 名称: %s | 初始投入: %.2f元 | 当前市值: %.2f元 | 预期年化: %s | 状态: %s | 开始: %s%s\n备注: %s",
                plan.getName(),
                plan.getInitialAmount() != null ? plan.getInitialAmount() : 0,
                plan.getCurrentValue() != null ? plan.getCurrentValue() : 0,
                plan.getExpectedRoi() != null ? plan.getExpectedRoi() + "%" : "未设置",
                plan.getStatus() != null && plan.getStatus() == 1 ? "已赎回" : "持有中",
                plan.getStartDate() != null ? plan.getStartDate().toString() : "未知",
                plan.getEndDate() != null ? " | 结束: " + plan.getEndDate() : "",
                plan.getRemark() != null ? plan.getRemark() : "");

        Map<String, Object> metadata = baseMetadata("plan", plan.getId(), plan.getUserId());
        metadata.put("title", plan.getName());
        metadata.put("detail", truncate(plan.getRemark(), DETAIL_MAX));
        return new Document(content, metadata);
    }

    // ===== 备忘录 =====

    public List<Document> chunkMemos(List<Memo> memos) {
        List<Document> docs = new ArrayList<>();
        for (Memo memo : memos) {
            String content = memo.getContent() != null ? memo.getContent() : "";
            if (content.length() <= SPLIT_THRESHOLD) {
                docs.add(buildMemoDoc(memo, content, null));
            } else {
                // 长备忘：滑动窗口二次切分
                List<String> parts = slidingWindow(content);
                for (int i = 0; i < parts.size(); i++) {
                    docs.add(buildMemoDoc(memo, parts.get(i), (i + 1) + "/" + parts.size()));
                }
            }
        }
        return docs;
    }

    public Document chunkMemo(Memo memo) {
        return buildMemoDoc(memo, memo.getContent() != null ? memo.getContent() : "", null);
    }

    private Document buildMemoDoc(Memo memo, String content, String part) {
        String text = String.format(
                "备忘录 | 日期: %s | 标题: %s%s\n内容: %s",
                memo.getCreateTime() != null ? memo.getCreateTime().format(DATE) : "未知",
                memo.getTitle(),
                part != null ? "（第" + part + "段）" : "",
                content != null ? content : "");

        Map<String, Object> metadata = baseMetadata("memo", memo.getId(), memo.getUserId());
        metadata.put("title", memo.getTitle() + (part != null ? "（" + part + "）" : ""));
        metadata.put("detail", truncate(content, DETAIL_MAX));
        return new Document(text, metadata);
    }

    // ===== 内部方法 =====

    /** 公共 metadata：类型 / 实体ID / 用户ID（检索时按 userId 过滤隔离） */
    private Map<String, Object> baseMetadata(String type, Long refId, Long userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", type);
        metadata.put("refId", refId);
        metadata.put("userId", userId);
        return metadata;
    }

    /** 滑动窗口切分长文本 */
    private List<String> slidingWindow(String text) {
        List<String> parts = new ArrayList<>();
        int step = CHUNK_SIZE - CHUNK_OVERLAP;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            parts.add(text.substring(start, end));
            if (end == text.length()) break;
        }
        return parts;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String flat = text.replace("\n", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }
}
