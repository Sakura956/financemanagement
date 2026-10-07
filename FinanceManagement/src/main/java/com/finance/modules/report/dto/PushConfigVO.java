package com.finance.modules.report.dto;

import lombok.Data;

/**
 * 推送配置返回
 */
@Data
public class PushConfigVO {

    /** 接收邮箱（未配置时为空串） */
    private String email;

    /** 报告推送开关 */
    private boolean weeklyEnabled;

    /** 发送频率：DAILY-每日, WEEKLY-每周 */
    private String frequency;

    /** 每周几发送（WEEKLY时生效）：1-周一 ... 7-周日 */
    private int dayOfWeek;

    /** 发送时间（小时，0-23） */
    private int sendHour;

    /** 上次发送时间 yyyy-MM-dd HH:mm（从未发送为空串） */
    private String lastSendTime;
}
