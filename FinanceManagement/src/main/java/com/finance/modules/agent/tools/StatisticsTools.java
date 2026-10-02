package com.finance.modules.agent.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.bill.entity.BillCategory;
import com.finance.modules.bill.entity.BillRecord;
import com.finance.modules.bill.mapper.BillCategoryMapper;
import com.finance.modules.bill.mapper.BillRecordMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计工具集（月度概览 / 分类占比 / 趋势）
 */
@Component
public class StatisticsTools {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BillRecordMapper billRecordMapper;
    private final BillCategoryMapper billCategoryMapper;

    public StatisticsTools(BillRecordMapper billRecordMapper, BillCategoryMapper billCategoryMapper) {
        this.billRecordMapper = billRecordMapper;
        this.billCategoryMapper = billCategoryMapper;
    }

    @Tool(description = "获取当前用户某个月的收支概览：总收入、总支出、结余。" +
            "适用场景：'我这个月结余多少'、'上个月收支情况'。yearMonth 不传默认当月。")
    public String getMonthlyOverview(
            @ToolParam(description = "月份，格式yyyy-MM，如 2026-09；不传默认当月", required = false) String yearMonth,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        String month = normalizeMonth(yearMonth);
        if (month == null) {
            return "失败：月份格式错误，应为yyyy-MM，例如 2026-09";
        }

        double income = 0, expense = 0;
        for (BillRecord r : monthRecords(userId, month)) {
            if (r.getType() != null && r.getAmount() != null) {
                if (r.getType() == 1) income += r.getAmount();
                else expense += r.getAmount();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("income", Math.round(income * 100.0) / 100.0);
        result.put("expense", Math.round(expense * 100.0) / 100.0);
        result.put("balance", Math.round((income - expense) * 100.0) / 100.0);
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "获取当前用户某个月各分类的金额和占比（饼图数据），可看支出占比或收入构成。" +
            "适用场景：'这个月哪类花得最多'、'上个月支出都花在哪了'。type：0-支出（默认），1-收入。")
    public String getCategoryDistribution(
            @ToolParam(description = "月份，格式yyyy-MM；不传默认当月", required = false) String yearMonth,
            @ToolParam(description = "统计类型：0-支出（默认），1-收入", required = false) Integer type,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        String month = normalizeMonth(yearMonth);
        if (month == null) {
            return "失败：月份格式错误，应为yyyy-MM，例如 2026-09";
        }
        int t = (type != null && type == 1) ? 1 : 0;

        // 按分类分组汇总
        Map<Long, Double> grouped = monthRecords(userId, month).stream()
                .filter(r -> r.getType() != null && r.getType() == t && r.getAmount() != null)
                .collect(Collectors.groupingBy(BillRecord::getCategoryId,
                        Collectors.summingDouble(BillRecord::getAmount)));

        double total = grouped.values().stream().mapToDouble(Double::doubleValue).sum();

        // 分类名称映射
        Map<Long, String> names = grouped.isEmpty() ? Map.of()
                : billCategoryMapper.selectBatchIds(grouped.keySet()).stream()
                .collect(Collectors.toMap(BillCategory::getId, BillCategory::getName));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", names.getOrDefault(entry.getKey(), "未知"));
            item.put("amount", Math.round(entry.getValue() * 100.0) / 100.0);
            item.put("percent", total > 0
                    ? String.format("%.1f%%", entry.getValue() / total * 100) : "0%");
            items.add(item);
        }
        items.sort(Comparator.comparingDouble(i -> -Double.parseDouble(String.valueOf(i.get("amount")))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("type", t == 1 ? "收入" : "支出");
        result.put("totalAmount", Math.round(total * 100.0) / 100.0);
        result.put("items", items);
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "获取当前用户近 N 个月的收支趋势，每月总收入、总支出、结余。" +
            "适用场景：'最近半年支出趋势怎么样'、'最近几个月的收支变化'。months 默认 6，最大 24。")
    public String getTrend(
            @ToolParam(description = "统计的月数，默认6，最大24", required = false) Integer months,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        int n = months == null || months < 1 ? 6 : Math.min(months, 24);

        List<Map<String, Object>> items = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = n - 1; i >= 0; i--) {
            String month = current.minusMonths(i).format(YM);
            double income = 0, expense = 0;
            for (BillRecord r : monthRecords(userId, month)) {
                if (r.getType() != null && r.getAmount() != null) {
                    if (r.getType() == 1) income += r.getAmount();
                    else expense += r.getAmount();
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("income", Math.round(income * 100.0) / 100.0);
            item.put("expense", Math.round(expense * 100.0) / 100.0);
            item.put("balance", Math.round((income - expense) * 100.0) / 100.0);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", n);
        result.put("trend", items);
        return JSONUtil.toJsonStr(result);
    }

    // ===== 内部方法 =====

    /** 月份归一化：null → 当月；格式错误 → null */
    private String normalizeMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now().format(YM);
        }
        try {
            return YearMonth.parse(yearMonth.trim(), YM).format(YM);
        } catch (Exception e) {
            return null;
        }
    }

    /** 查询某月账单记录（与统计服务同款时间范围逻辑，直连数据库保证实时准确） */
    private List<BillRecord> monthRecords(Long userId, String month) {
        YearMonth ym = YearMonth.parse(month, YM);
        return billRecordMapper.selectList(
                new LambdaQueryWrapper<BillRecord>()
                        .eq(BillRecord::getUserId, userId)
                        .ge(BillRecord::getRecordTime, ym.atDay(1).atStartOfDay())
                        .le(BillRecord::getRecordTime, ym.atEndOfMonth().atTime(23, 59, 59)));
    }
}
