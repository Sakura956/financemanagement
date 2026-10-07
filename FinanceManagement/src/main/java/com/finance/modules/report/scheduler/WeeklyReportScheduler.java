package com.finance.modules.report.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.report.entity.ReportPushConfig;
import com.finance.modules.report.mapper.ReportPushConfigMapper;
import com.finance.modules.report.service.WeeklyReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务报告定时推送
 *
 * 每小时整点运行一次，逐个检查开启推送的用户：
 * - DAILY  ：send_hour 与当前小时相同即触发；
 * - WEEKLY ：send_hour 与当前小时相同 且 今天是配置的 day_of_week；
 * 当天已发过（last_send_time 为今天）则跳过，避免重复。
 * 发送时间完全由用户在个人设置中自定义。
 */
@Component
@EnableScheduling
public class WeeklyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportScheduler.class);

    private final ReportPushConfigMapper configMapper;
    private final WeeklyReportService weeklyReportService;

    @Value("${report.push.enabled:true}")
    private boolean enabled;

    public WeeklyReportScheduler(ReportPushConfigMapper configMapper,
                                 WeeklyReportService weeklyReportService) {
        this.configMapper = configMapper;
        this.weeklyReportService = weeklyReportService;
    }

    /**
     * 每小时整点检查各用户的自定义发送计划
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sendScheduledReports() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ReportPushConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<ReportPushConfig>()
                        .eq(ReportPushConfig::getWeeklyEnabled, 1));
        if (configs.isEmpty()) {
            return;
        }

        int hour = now.getHour();
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue(); // 1-周一 ... 7-周日
        LocalDate today = now.toLocalDate();

        int sent = 0;
        for (ReportPushConfig config : configs) {
            if (!shouldSendNow(config, hour, dayOfWeek, today)) {
                continue;
            }
            try {
                weeklyReportService.generateAndSend(config.getUserId());
                sent++;
            } catch (Exception e) {
                // 单用户失败不中断整批任务（失败详情已落库 weekly_report）
                log.error("[Report] 用户 {} 定时报告发送异常: {}", config.getUserId(), e.getMessage());
            }
        }
        if (sent > 0) {
            log.info("[Report] 定时报告发送完成: {} 位用户（本轮检查 {} 位开启推送）", sent, configs.size());
        }
    }

    /** 判断该用户是否应在当前小时发送 */
    private boolean shouldSendNow(ReportPushConfig config, int hour, int dayOfWeek, LocalDate today) {
        Integer sendHour = config.getSendHour();
        if (sendHour == null || sendHour != hour) {
            return false;
        }
        // 当天已发过则跳过
        if (config.getLastSendTime() != null
                && config.getLastSendTime().toLocalDate().equals(today)) {
            return false;
        }
        // 每日：小时匹配即发；每周：还需匹配周几
        if (ReportPushConfig.FREQ_DAILY.equals(config.getFrequency())) {
            return true;
        }
        Integer dow = config.getDayOfWeek();
        return dow == null || dow == dayOfWeek;
    }
}
