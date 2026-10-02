package com.finance.modules.agent.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.agent.rag.RagService;
import com.finance.modules.plan.entity.FinancePlan;
import com.finance.modules.plan.mapper.FinancePlanMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务计划工具集（理财计划 / 预算目标）
 */
@Component
public class PlanTools {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FinancePlanMapper financePlanMapper;
    private final RagService ragService;

    public PlanTools(FinancePlanMapper financePlanMapper, RagService ragService) {
        this.financePlanMapper = financePlanMapper;
        this.ragService = ragService;
    }

    @Tool(description = "查询当前用户的财务计划/理财计划列表，含每个计划的投入、市值、收益和收益率，以及总体汇总。" +
            "适用场景：'我有哪些理财计划'、'我持有的基金收益怎么样'。status：0-持有中，1-已赎回；不传查全部。")
    public String listPlans(
            @ToolParam(description = "计划状态：0-持有中，1-已赎回；不传查全部", required = false) Integer status,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        LambdaQueryWrapper<FinancePlan> wrapper = new LambdaQueryWrapper<FinancePlan>()
                .eq(FinancePlan::getUserId, userId);
        if (status != null && (status == 0 || status == 1)) {
            wrapper.eq(FinancePlan::getStatus, status);
        }
        List<FinancePlan> plans = financePlanMapper.selectList(wrapper.orderByDesc(FinancePlan::getCreateTime));

        double totalInvested = 0, totalValue = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        for (FinancePlan p : plans) {
            double initial = p.getInitialAmount() != null ? p.getInitialAmount() : 0;
            double current = p.getCurrentValue() != null ? p.getCurrentValue() : 0;
            double profit = current - initial;
            totalInvested += initial;
            totalValue += current;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("status", p.getStatus() != null && p.getStatus() == 1 ? "已赎回" : "持有中");
            item.put("initialAmount", initial);
            item.put("currentValue", current);
            item.put("profitAmount", Math.round(profit * 100.0) / 100.0);
            item.put("profitRate", initial > 0 ? Math.round(profit / initial * 10000.0) / 100.0 : 0);
            item.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
            item.put("remark", p.getRemark());
            items.add(item);
        }

        double totalProfit = totalValue - totalInvested;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("records", items);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalInvested", Math.round(totalInvested * 100.0) / 100.0);
        summary.put("totalCurrentValue", Math.round(totalValue * 100.0) / 100.0);
        summary.put("totalProfit", Math.round(totalProfit * 100.0) / 100.0);
        summary.put("overallProfitRate", totalInvested > 0
                ? Math.round(totalProfit / totalInvested * 10000.0) / 100.0 : 0);
        result.put("summary", summary);
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "为当前用户创建一个财务计划或预算目标。" +
            "适用场景：'帮我设一个每月餐饮预算1500'、'我买了1万元基金，现在市值10500'。" +
            "initialAmount 为初始投入/预算金额（必须大于0），currentValue 为当前市值（默认等于 initialAmount），remark 填计划说明。")
    public String createPlan(
            @ToolParam(description = "计划名称，如'餐饮预算'、'沪深300指数基金'") String name,
            @ToolParam(description = "初始投入金额或预算金额，正数，单位元") double initialAmount,
            @ToolParam(description = "当前市值，正数；不传默认等于初始投入金额", required = false) Double currentValue,
            @ToolParam(description = "预期年化收益率(%)，可null", required = false) Double expectedRoi,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd；不传默认今天", required = false) String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd；可null", required = false) String endDate,
            @ToolParam(description = "备注说明，如'每月餐饮预算控制在1500以内'；可null", required = false) String remark,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        if (!StringUtils.hasText(name) || name.length() > 50) {
            return "失败：计划名称不能为空且不超过50字";
        }
        if (initialAmount <= 0) {
            return "失败：初始投入金额必须大于0";
        }

        FinancePlan plan = new FinancePlan();
        plan.setUserId(userId);
        plan.setName(name.trim());
        plan.setInitialAmount(initialAmount);
        plan.setCurrentValue(currentValue != null ? currentValue : initialAmount);
        plan.setExpectedRoi(expectedRoi);
        try {
            plan.setStartDate(StringUtils.hasText(startDate)
                    ? LocalDate.parse(startDate.trim(), DATE) : LocalDate.now());
            if (StringUtils.hasText(endDate)) {
                plan.setEndDate(LocalDate.parse(endDate.trim(), DATE));
            }
        } catch (Exception e) {
            return "失败：日期格式错误，应为yyyy-MM-dd，例如 2026-10-01";
        }
        plan.setRemark(remark);
        plan.setStatus(0);
        financePlanMapper.insert(plan);

        ragService.indexPlan(plan); // 增量更新向量索引

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", plan.getId());
        result.put("message", "已创建计划'" + plan.getName() +
                "'（初始投入 " + plan.getInitialAmount() + "元，当前市值 " + plan.getCurrentValue() + "元）");
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "查询某个财务计划的当前估值、收益金额和收益率。" +
            "适用场景：'我的XX基金赚了多少'。需要计划ID，用户没提供时先用 listPlans 查询。")
    public String getPlanValuation(
            @ToolParam(description = "财务计划ID（数字）") Long planId,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        if (planId == null) {
            return "失败：缺少计划ID，请先用 listPlans 查询";
        }
        FinancePlan plan = financePlanMapper.selectOne(
                new LambdaQueryWrapper<FinancePlan>()
                        .eq(FinancePlan::getId, planId)
                        .eq(FinancePlan::getUserId, userId));
        if (plan == null) {
            return "失败：计划不存在或不属于当前用户，ID=" + planId;
        }

        double initial = plan.getInitialAmount() != null ? plan.getInitialAmount() : 0;
        double current = plan.getCurrentValue() != null ? plan.getCurrentValue() : 0;
        double profit = current - initial;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", plan.getId());
        result.put("name", plan.getName());
        result.put("status", plan.getStatus() != null && plan.getStatus() == 1 ? "已赎回" : "持有中");
        result.put("initialAmount", initial);
        result.put("currentValue", current);
        result.put("profitAmount", Math.round(profit * 100.0) / 100.0);
        result.put("profitRate", initial > 0 ? Math.round(profit / initial * 10000.0) / 100.0 : 0);
        return JSONUtil.toJsonStr(result);
    }
}
