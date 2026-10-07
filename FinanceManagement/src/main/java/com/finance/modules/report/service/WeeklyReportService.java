package com.finance.modules.report.service;

import com.finance.modules.report.dto.PushConfigRequest;
import com.finance.modules.report.dto.PushConfigVO;
import com.finance.modules.report.dto.ReportPreviewVO;
import com.finance.modules.report.entity.WeeklyReport;

import java.util.List;

/**
 * 每周财务报告服务
 */
public interface WeeklyReportService {

    /**
     * 获取当前用户推送配置（无配置返回默认空值）
     */
    PushConfigVO getConfig(Long userId);

    /**
     * 保存推送配置（邮箱 + 开关）
     */
    void saveConfig(Long userId, PushConfigRequest request);

    /**
     * 发送测试邮件（需已保存邮箱）
     */
    void sendTestEmail(Long userId);

    /**
     * 生成并预览本周报告（不发送、不落库）
     */
    ReportPreviewVO previewReport(Long userId);

    /**
     * 最近 10 份存档报告
     */
    List<WeeklyReport> listReports(Long userId);

    /**
     * 立即生成并发送一份报告到用户邮箱（失败抛业务异常，前端可见原因）
     */
    void sendNow(Long userId);

    /**
     * 生成周报并发送 + 落库存档（定时任务入口，单用户异常不影响其他用户）
     */
    void generateAndSend(Long userId);
}
