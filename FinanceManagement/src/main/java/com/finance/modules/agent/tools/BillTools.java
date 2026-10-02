package com.finance.modules.agent.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.bill.entity.BillCategory;
import com.finance.modules.bill.entity.BillRecord;
import com.finance.modules.bill.mapper.BillCategoryMapper;
import com.finance.modules.bill.mapper.BillRecordMapper;
import com.finance.modules.agent.rag.RagService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账单工具集（AI 通过自然语言自动决策调用）
 *
 * 注意：所有方法通过 ToolContext 获取当前用户ID（流式模式下 SecurityContext 不可用），
 * 直连 Mapper 完成读写，保证用户数据隔离。
 */
@Component
public class BillTools {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BillRecordMapper billRecordMapper;
    private final BillCategoryMapper billCategoryMapper;
    private final AgentCacheCleaner cacheCleaner;
    private final RagService ragService;

    public BillTools(BillRecordMapper billRecordMapper,
                     BillCategoryMapper billCategoryMapper,
                     AgentCacheCleaner cacheCleaner,
                     RagService ragService) {
        this.billRecordMapper = billRecordMapper;
        this.billCategoryMapper = billCategoryMapper;
        this.cacheCleaner = cacheCleaner;
        this.ragService = ragService;
    }

    @Tool(description = "查询当前用户的账单记录，支持按分类名称、日期范围、关键词、收支类型筛选。" +
            "适用场景：'我这个月餐饮花了多少'、'最近有哪些外卖支出'、'上个月的工资收入'。" +
            "参数全部可选，返回符合条件的明细列表和总金额汇总。")
    public String queryBills(
            @ToolParam(description = "账单分类名称，如'餐饮'、'交通'、'工资'；不传表示所有分类", required = false) String category,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd（包含当天）；不传表示不限", required = false) String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd（包含当天）；不传表示不限", required = false) String endDate,
            @ToolParam(description = "关键词，模糊匹配账单备注描述；不传表示不限", required = false) String keyword,
            @ToolParam(description = "账单类型：0-支出，1-收入；不传表示全部", required = false) Integer type,
            @ToolParam(description = "最多返回的明细条数，默认50，最大200（汇总金额不受此限制）", required = false) Integer limit,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        int maxRows = limit == null || limit < 1 ? 50 : Math.min(limit, 200);

        LambdaQueryWrapper<BillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillRecord::getUserId, userId);
        try {
            if (StringUtils.hasText(category)) {
                List<Long> categoryIds = matchCategoryIds(category);
                if (categoryIds.isEmpty()) {
                    return "未找到分类'" + category + "'。可选分类：" + listCategoryNames();
                }
                wrapper.in(BillRecord::getCategoryId, categoryIds);
            }
            if (StringUtils.hasText(startDate)) {
                wrapper.ge(BillRecord::getRecordTime, LocalDate.parse(startDate.trim(), DATE).atStartOfDay());
            }
            if (StringUtils.hasText(endDate)) {
                wrapper.le(BillRecord::getRecordTime, LocalDate.parse(endDate.trim(), DATE).atTime(23, 59, 59));
            }
        } catch (Exception e) {
            return "日期格式错误，应为yyyy-MM-dd，例如 2026-09-30";
        }
        if (type != null && (type == 0 || type == 1)) {
            wrapper.eq(BillRecord::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(BillRecord::getDescription, keyword.trim());
        }

        // 全量查询（个人账本数据量小，一次查出后 Java 端汇总，保证"一共花了多少"准确）
        List<BillRecord> all = billRecordMapper.selectList(
                wrapper.orderByDesc(BillRecord::getRecordTime));

        double totalExpense = 0, totalIncome = 0;
        for (BillRecord r : all) {
            if (r.getType() != null && r.getAmount() != null) {
                if (r.getType() == 1) totalIncome += r.getAmount();
                else totalExpense += r.getAmount();
            }
        }

        // 明细只展示前 N 条
        List<BillRecord> records = all.size() > maxRows ? all.subList(0, maxRows) : all;

        // 分类名称映射（一次查出，避免 N+1）
        Map<Long, String> categoryNames = loadCategoryNames(records);

        List<Map<String, Object>> items = new ArrayList<>();
        for (BillRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("date", r.getRecordTime() != null ? r.getRecordTime().toLocalDate().toString() : null);
            item.put("type", r.getType() == 1 ? "收入" : "支出");
            item.put("category", categoryNames.getOrDefault(r.getCategoryId(), "未知"));
            item.put("amount", r.getAmount());
            item.put("description", r.getDescription());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchedCount", all.size());
        result.put("totalExpense", Math.round(totalExpense * 100.0) / 100.0);
        result.put("totalIncome", Math.round(totalIncome * 100.0) / 100.0);
        result.put("records", items);
        result.put("note", all.size() > maxRows ? "明细仅展示前" + maxRows + "条，汇总金额为全部符合条件数据之和" : "");
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "为当前用户记录一笔新账单（支出或收入）。" +
            "适用场景：'昨天沙县小吃花了28块'、'这个月发工资12000元'。" +
            "分类名称会在系统分类表中匹配（如'餐饮'、'交通'），金额必须为正数，日期不传默认今天。")
    public String createBill(
            @ToolParam(description = "账单描述/备注，如'沙县小吃午饭'、'10月房租'") String description,
            @ToolParam(description = "金额，正数，单位元") double amount,
            @ToolParam(description = "账单类型：0-支出，1-收入；不传默认0-支出", required = false) Integer type,
            @ToolParam(description = "分类名称，如'餐饮'、'交通'、'工资'。若不确定可先不传，系统会返回可选分类列表", required = false) String category,
            @ToolParam(description = "消费/发生日期，格式yyyy-MM-dd；不传默认今天", required = false) String date,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        if (amount <= 0) {
            return "失败：金额必须大于0";
        }
        int billType = (type != null && type == 1) ? 1 : 0;

        // 分类解析：先精确匹配，再模糊匹配
        BillCategory billCategory = resolveCategory(category, billType);
        if (billCategory == null) {
            return StringUtils.hasText(category)
                    ? "失败：无法匹配分类'" + category + "'。请从以下分类中选择：" + listCategoryNames()
                    : "失败：未指定分类。请从以下分类中选择：" + listCategoryNames();
        }

        // 日期解析：默认今天
        LocalDateTime recordTime;
        try {
            recordTime = StringUtils.hasText(date)
                    ? LocalDate.parse(date.trim(), DATE).atTime(12, 0, 0)
                    : LocalDateTime.now();
        } catch (Exception e) {
            return "失败：日期格式错误，应为yyyy-MM-dd，例如 2026-09-30";
        }
        if (recordTime.isAfter(LocalDateTime.now())) {
            return "失败：记录时间不能是未来时间";
        }

        BillRecord record = new BillRecord();
        record.setUserId(userId);
        record.setType(billType);
        record.setAmount(amount);
        record.setCategoryId(billCategory.getId());
        record.setDescription(description);
        record.setRecordTime(recordTime);
        billRecordMapper.insert(record);

        cacheCleaner.clearBillAndStatsCache(userId);      // 清理账单/统计缓存
        ragService.indexBill(record, billCategory.getName()); // 增量更新向量索引

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", record.getId());
        result.put("message", String.format("已记录：%s %s%s %.2f元（%s）",
                recordTime.toLocalDate(), billCategory.getName(),
                billType == 1 ? "收入" : "支出", amount,
                StringUtils.hasText(description) ? description : "无备注"));
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "删除当前用户的一笔账单。适用场景：'帮我删掉那条记错的账单'。" +
            "需要账单ID，如果用户没有提供ID，先用 queryBills 查询后向用户确认。")
    public String deleteBill(
            @ToolParam(description = "要删除的账单ID（数字）") Long billId,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        if (billId == null) {
            return "失败：缺少账单ID，请先用 queryBills 查询确认要删除的账单";
        }
        BillRecord record = billRecordMapper.selectOne(
                new LambdaQueryWrapper<BillRecord>()
                        .eq(BillRecord::getId, billId)
                        .eq(BillRecord::getUserId, userId));
        if (record == null) {
            return "失败：账单不存在或不属于当前用户，ID=" + billId;
        }
        billRecordMapper.deleteById(billId);
        cacheCleaner.clearBillAndStatsCache(userId);
        ragService.removeBill(billId);

        return "已删除账单 ID=" + billId
                + "（" + record.getRecordTime().toLocalDate() + " " + record.getAmount() + "元）";
    }

    // ===== 内部方法 =====

    /** 按名称精确→模糊匹配分类ID */
    private List<Long> matchCategoryIds(String name) {
        List<BillCategory> exact = billCategoryMapper.selectList(
                new LambdaQueryWrapper<BillCategory>().eq(BillCategory::getName, name.trim()));
        if (!exact.isEmpty()) {
            return exact.stream().map(BillCategory::getId).toList();
        }
        return billCategoryMapper.selectList(
                        new LambdaQueryWrapper<BillCategory>().like(BillCategory::getName, name.trim()))
                .stream().map(BillCategory::getId).toList();
    }

    /** 解析单个分类（创建账单用），先精确后模糊 */
    private BillCategory resolveCategory(String name, int type) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        BillCategory exact = billCategoryMapper.selectOne(new LambdaQueryWrapper<BillCategory>()
                .eq(BillCategory::getType, type)
                .eq(BillCategory::getStatus, 0)
                .eq(BillCategory::getName, name.trim()));
        if (exact != null) return exact;
        return billCategoryMapper.selectOne(new LambdaQueryWrapper<BillCategory>()
                .eq(BillCategory::getType, type)
                .eq(BillCategory::getStatus, 0)
                .like(BillCategory::getName, name.trim())
                .last("LIMIT 1"));
    }

    /** 列出可选分类名称（按类型分组） */
    private String listCategoryNames() {
        Map<Integer, List<String>> grouped = billCategoryMapper.selectList(
                        new LambdaQueryWrapper<BillCategory>().eq(BillCategory::getStatus, 0)
                                .orderByAsc(BillCategory::getSortOrder))
                .stream().collect(Collectors.groupingBy(BillCategory::getType,
                        Collectors.mapping(BillCategory::getName, Collectors.toList())));
        List<String> expense = grouped.getOrDefault(0, List.of());
        List<String> income = grouped.getOrDefault(1, List.of());
        return "支出[" + String.join("、", expense) + "]；收入[" + String.join("、", income) + "]";
    }

    /** 批量加载分类名称映射 */
    private Map<Long, String> loadCategoryNames(List<BillRecord> records) {
        List<Long> ids = records.stream()
                .map(BillRecord::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return billCategoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BillCategory::getId, BillCategory::getName));
    }
}
