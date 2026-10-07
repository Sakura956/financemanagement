package com.finance.modules.report.dto;

import lombok.Data;

/**
 * 保存推送配置请求
 */
@Data
public class PushConfigRequest {

    /** 接收邮箱 */
    private String email;

    /** 报告推送开关：true-开启, false-关闭 */
    private Boolean weeklyEnabled;

    /** 发送频率：DAILY-每日, WEEKLY-每周（不传默认 WEEKLY） */
    private String frequency;

    /** 每周几发送（frequency=WEEKLY 时生效）：1-周一 ... 7-周日（不传默认 7-周日） */
    private Integer dayOfWeek;

    /** 发送时间（小时，0-23；不传默认 20） */
    private Integer sendHour;
}
