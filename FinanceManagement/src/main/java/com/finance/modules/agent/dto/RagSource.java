package com.finance.modules.agent.dto;

import lombok.Data;

/**
 * RAG 检索来源（前端"参考来源"溯源卡片）
 */
@Data
public class RagSource {

    /** 来源类型：bill-账单 / plan-财务计划 / memo-备忘录 */
    private String type;

    /** 来源实体ID */
    private Long refId;

    /** 展示标题，如 "2026-09-30 餐饮支出 28.00元" */
    private String title;

    /** 补充说明（账单备注/计划备注/备忘内容，截断） */
    private String detail;
}
