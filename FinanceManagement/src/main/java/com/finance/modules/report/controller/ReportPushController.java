package com.finance.modules.report.controller;

import com.finance.common.result.Result;
import com.finance.modules.report.dto.PushConfigRequest;
import com.finance.modules.report.dto.PushConfigVO;
import com.finance.modules.report.dto.ReportPreviewVO;
import com.finance.modules.report.entity.WeeklyReport;
import com.finance.modules.report.service.WeeklyReportService;
import com.finance.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 周报推送接口（邮箱配置 / 测试发送 / 预览 / 历史报告）
 */
@RestController
@RequestMapping("/api/v1/user/report")
public class ReportPushController {

    private final WeeklyReportService weeklyReportService;

    public ReportPushController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    /**
     * 获取当前用户推送配置
     */
    @GetMapping("/push-config")
    public Result<PushConfigVO> getConfig() {
        return Result.success(weeklyReportService.getConfig(SecurityUtil.getCurrentUserId()));
    }

    /**
     * 保存推送配置（邮箱 + 每周开关）
     */
    @PutMapping("/push-config")
    public Result<Void> saveConfig(@RequestBody PushConfigRequest request) {
        weeklyReportService.saveConfig(SecurityUtil.getCurrentUserId(), request);
        return Result.success("推送配置保存成功", null);
    }

    /**
     * 发送测试邮件（需已保存邮箱）
     */
    @PostMapping("/push-config/test")
    public Result<Void> sendTestEmail() {
        weeklyReportService.sendTestEmail(SecurityUtil.getCurrentUserId());
        return Result.success("测试邮件已发送，请查收", null);
    }

    /**
     * 立即生成并发送一份报告到邮箱（失败返回具体原因）
     */
    @PostMapping("/send-now")
    public Result<Void> sendNow() {
        weeklyReportService.sendNow(SecurityUtil.getCurrentUserId());
        return Result.success("报告已生成并发送到你的邮箱", null);
    }

    /**
     * 生成并预览本周报告（不发送、不落库）
     */
    @PostMapping("/preview")
    public Result<ReportPreviewVO> previewReport() {
        return Result.success(weeklyReportService.previewReport(SecurityUtil.getCurrentUserId()));
    }

    /**
     * 最近 10 份存档报告
     */
    @GetMapping("/list")
    public Result<List<WeeklyReport>> listReports() {
        return Result.success(weeklyReportService.listReports(SecurityUtil.getCurrentUserId()));
    }
}
