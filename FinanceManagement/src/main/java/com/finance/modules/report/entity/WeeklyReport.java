package com.finance.modules.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每周财务报告存档（AI 生成的 Markdown 周报）
 */
@Data
@TableName("weekly_report")
public class WeeklyReport {

    /** 发送状态：未发送 */
    public static final int STATUS_NOT_SENT = 0;
    /** 发送状态：成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 发送状态：失败 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;//报告ID

    private Long userId;//用户ID

    private String title;//报告标题

    private String content;//报告内容（Markdown）

    private LocalDate periodStart;//统计开始日期

    private LocalDate periodEnd;//统计结束日期

    private Integer sendStatus;//发送状态：0-未发送, 1-成功, 2-失败

    private String errorMsg;//失败原因

    private LocalDateTime createTime;//创建时间
}
