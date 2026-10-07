package com.finance.modules.report.dto;

import lombok.Data;

/**
 * 周报预览返回（Markdown 内容，不发送）
 */
@Data
public class ReportPreviewVO {

    /** 报告标题 */
    private String title;

    /** 报告内容（Markdown） */
    private String content;

    /** 统计开始日期 yyyy-MM-dd */
    private String periodStart;

    /** 统计结束日期 yyyy-MM-dd */
    private String periodEnd;
}
