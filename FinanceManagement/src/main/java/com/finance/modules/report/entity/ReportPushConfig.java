package com.finance.modules.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报告推送配置（每个用户一条，配置接收报告的邮箱、频率与发送时间）
 */
@Data
@TableName("report_push_config")
public class ReportPushConfig {

    /** 发送频率：每日 */
    public static final String FREQ_DAILY = "DAILY";
    /** 发送频率：每周 */
    public static final String FREQ_WEEKLY = "WEEKLY";

    @TableId(type = IdType.AUTO)
    private Long id;//配置ID

    private Long userId;//用户ID

    private String email;//接收邮箱

    private Integer weeklyEnabled;//报告推送开关：0-关闭, 1-开启

    private String frequency;//发送频率：DAILY-每日, WEEKLY-每周

    private Integer dayOfWeek;//每周几发送（WEEKLY时生效）：1-周一 ... 7-周日

    private Integer sendHour;//发送时间（小时，0-23）

    private LocalDateTime lastSendTime;//上次发送时间

    private LocalDateTime createTime;//创建时间

    private LocalDateTime updateTime;//更新时间
}
