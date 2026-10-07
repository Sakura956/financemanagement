package com.finance.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.common.exception.BusinessException;
import com.finance.modules.agent.tools.AgentToolSupport;
import com.finance.modules.agent.tools.BillTools;
import com.finance.modules.agent.tools.PlanTools;
import com.finance.modules.agent.tools.StatisticsTools;
import com.finance.modules.report.dto.PushConfigRequest;
import com.finance.modules.report.dto.PushConfigVO;
import com.finance.modules.report.dto.ReportPreviewVO;
import com.finance.modules.report.entity.ReportPushConfig;
import com.finance.modules.report.entity.WeeklyReport;
import com.finance.modules.report.mapper.ReportPushConfigMapper;
import com.finance.modules.report.mapper.WeeklyReportMapper;
import com.finance.modules.report.service.ResendEmailService;
import com.finance.modules.report.service.WeeklyReportService;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 每周财务报告服务实现
 *
 * 流程（复用 Agent 工具取数，零重复查询逻辑）：
 * 构造 ToolContext(userId) → 直调 BillTools/StatisticsTools/PlanTools
 * → 拼数据喂给 reportChatClient 生成 Markdown → commonmark 转 HTML → Resend 发送 → 落库存档。
 *
 * 注意：定时任务线程无登录态，userId 一律显式传参（与 AgentServiceImpl 同一设计）。
 */
@Service
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportServiceImpl.class);

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM.dd");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 简单邮箱格式校验 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String REPORT_SYSTEM_PROMPT = """
            你是一位专业的个人财务分析师，负责为用户撰写每周财务报告。

            撰写要求：
            1. 只基于提供的真实统计数据撰写，严禁编造或修改任何数字；
            2. 输出 Markdown 格式，结构固定为四部分：
               ## 本周收支总结
               ## 亮点与异常
               ## 趋势解读
               ## 下周建议
            3. "亮点与异常"需指出大额支出、分类占比异常、环比突增等值得关注的点；
            4. "趋势解读"结合近6个月收支趋势数据给出简短判断；
            5. "下周建议"给出2-3条，客观中立，必须提示不构成投资建议；
            6. 金额单位为元、保留两位小数，关键数字可加粗；总长度控制在500字以内；
            7. 使用简体中文，语气友好专业。""";

    private final ReportPushConfigMapper configMapper;
    private final WeeklyReportMapper reportMapper;
    private final ResendEmailService resendEmailService;
    private final ChatClient reportChatClient;
    private final BillTools billTools;
    private final StatisticsTools statisticsTools;
    private final PlanTools planTools;

    public WeeklyReportServiceImpl(ReportPushConfigMapper configMapper,
                                   WeeklyReportMapper reportMapper,
                                   ResendEmailService resendEmailService,
                                   @Qualifier("reportChatClient") ChatClient reportChatClient,
                                   BillTools billTools,
                                   StatisticsTools statisticsTools,
                                   PlanTools planTools) {
        this.configMapper = configMapper;
        this.reportMapper = reportMapper;
        this.resendEmailService = resendEmailService;
        this.reportChatClient = reportChatClient;
        this.billTools = billTools;
        this.statisticsTools = statisticsTools;
        this.planTools = planTools;
    }

    // ===== 推送配置 =====

    @Override
    public PushConfigVO getConfig(Long userId) {
        ReportPushConfig config = selectByUserId(userId);
        PushConfigVO vo = new PushConfigVO();
        if (config == null) {
            vo.setEmail("");
            vo.setWeeklyEnabled(false);
            vo.setFrequency(ReportPushConfig.FREQ_WEEKLY);
            vo.setDayOfWeek(7);
            vo.setSendHour(20);
            vo.setLastSendTime("");
            return vo;
        }
        vo.setEmail(config.getEmail());
        vo.setWeeklyEnabled(config.getWeeklyEnabled() != null && config.getWeeklyEnabled() == 1);
        vo.setFrequency(StringUtils.hasText(config.getFrequency())
                ? config.getFrequency() : ReportPushConfig.FREQ_WEEKLY);
        vo.setDayOfWeek(config.getDayOfWeek() != null && config.getDayOfWeek() >= 1 && config.getDayOfWeek() <= 7
                ? config.getDayOfWeek() : 7);
        vo.setSendHour(config.getSendHour() != null && config.getSendHour() >= 0 && config.getSendHour() <= 23
                ? config.getSendHour() : 20);
        vo.setLastSendTime(config.getLastSendTime() != null
                ? config.getLastSendTime().format(DATE_TIME) : "");
        return vo;
    }

    @Override
    public void saveConfig(Long userId, PushConfigRequest request) {
        if (request == null || !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("请填写接收邮箱");
        }
        String email = request.getEmail().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        boolean enabled = request.getWeeklyEnabled() == null || request.getWeeklyEnabled();

        // 发送计划归一化：频率 / 周几 / 小时
        String frequency = ReportPushConfig.FREQ_WEEKLY;
        if (StringUtils.hasText(request.getFrequency())) {
            frequency = request.getFrequency().trim().toUpperCase();
            if (!ReportPushConfig.FREQ_DAILY.equals(frequency)
                    && !ReportPushConfig.FREQ_WEEKLY.equals(frequency)) {
                throw new BusinessException("发送频率仅支持 DAILY（每日）或 WEEKLY（每周）");
            }
        }
        int dayOfWeek = (request.getDayOfWeek() != null && request.getDayOfWeek() >= 1 && request.getDayOfWeek() <= 7)
                ? request.getDayOfWeek() : 7;
        int sendHour = (request.getSendHour() != null && request.getSendHour() >= 0 && request.getSendHour() <= 23)
                ? request.getSendHour() : 20;

        ReportPushConfig config = selectByUserId(userId);
        if (config == null) {
            config = new ReportPushConfig();
            config.setUserId(userId);
            config.setEmail(email);
            config.setWeeklyEnabled(enabled ? 1 : 0);
            config.setFrequency(frequency);
            config.setDayOfWeek(dayOfWeek);
            config.setSendHour(sendHour);
            configMapper.insert(config);
        } else {
            config.setEmail(email);
            config.setWeeklyEnabled(enabled ? 1 : 0);
            config.setFrequency(frequency);
            config.setDayOfWeek(dayOfWeek);
            config.setSendHour(sendHour);
            configMapper.updateById(config);
        }
        log.info("[Report] 推送配置已保存: userId={}, email={}, enabled={}, frequency={}, dayOfWeek={}, sendHour={}",
                userId, email, enabled, frequency, dayOfWeek, sendHour);
    }

    @Override
    public void sendTestEmail(Long userId) {
        ReportPushConfig config = requireConfig(userId);
        String subject = "【FinanceAgent】测试邮件";
        String html = wrapEmailHtml("## 测试邮件\n\n这是一封测试邮件。收到它说明你的财务周报推送配置已生效，"
                + "每周日晚会按时收到 AI 生成的财务周报。");
        resendEmailService.send(config.getEmail(), subject, html);
    }

    // ===== 报告生成与发送 =====

    @Override
    public ReportPreviewVO previewReport(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String markdown = generateMarkdown(userId, start, end);

        ReportPreviewVO vo = new ReportPreviewVO();
        vo.setTitle(buildTitle(start, end));
        vo.setContent(markdown);
        vo.setPeriodStart(start.format(DATE));
        vo.setPeriodEnd(end.format(DATE));
        return vo;
    }

    @Override
    public List<WeeklyReport> listReports(Long userId) {
        return reportMapper.selectList(new LambdaQueryWrapper<WeeklyReport>()
                .eq(WeeklyReport::getUserId, userId)
                .orderByDesc(WeeklyReport::getCreateTime)
                .last("LIMIT 10"));
    }

    @Override
    public void sendNow(Long userId) {
        ReportPushConfig config = requireConfig(userId);
        generateAndSend(config);
    }

    @Override
    public void generateAndSend(Long userId) {
        ReportPushConfig config = selectByUserId(userId);
        if (config == null || !StringUtils.hasText(config.getEmail())) {
            log.info("[Report] 用户 {} 未配置推送邮箱，跳过报告生成", userId);
            return;
        }
        generateAndSend(config);
    }

    /**
     * 生成 + 落库 + 发送。
     * 生成失败/发送失败都会落库 status=2 记录并抛业务异常，
     * 供"立即发送"接口向前端回显原因；定时任务侧由调用方捕获。
     */
    private void generateAndSend(ReportPushConfig config) {
        Long userId = config.getUserId();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        String title = buildTitle(start, end);

        // 1. 生成 + 落库（先存为"未发送"）
        String markdown;
        try {
            markdown = generateMarkdown(userId, start, end);
        } catch (Exception e) {
            log.error("[Report] 用户 {} 报告生成失败: {}", userId, e.getMessage(), e);
            saveReport(userId, title, "生成失败", start, end,
                    WeeklyReport.STATUS_FAILED, "AI 生成失败：" + truncate(e.getMessage(), 400));
            throw new BusinessException("报告生成失败：" + e.getMessage());
        }

        WeeklyReport report = saveReport(userId, title, markdown, start, end,
                WeeklyReport.STATUS_NOT_SENT, null);

        // 2. 发送并回写状态
        try {
            String html = wrapEmailHtml(markdown);
            resendEmailService.send(config.getEmail(), "【FinanceAgent】" + title, html);
            report.setSendStatus(WeeklyReport.STATUS_SUCCESS);
            config.setLastSendTime(LocalDateTime.now());
            configMapper.updateById(config);
        } catch (Exception e) {
            log.warn("[Report] 用户 {} 报告发送失败: {}", userId, e.getMessage());
            report.setSendStatus(WeeklyReport.STATUS_FAILED);
            report.setErrorMsg(truncate(e.getMessage(), 500));
            reportMapper.updateById(report);
            throw new BusinessException(e.getMessage());
        }
        reportMapper.updateById(report);
    }

    // ===== 内部方法 =====

    /** 取数 + 调 ChatClient 生成 Markdown 周报 */
    private String generateMarkdown(Long userId, LocalDate start, LocalDate end) {
        Map<String, Object> ctxMap = Map.of(AgentToolSupport.CTX_USER_ID, userId);
        ToolContext ctx = new ToolContext(ctxMap);

        // 复用 Agent 工具直连查询（个人数据量小，返回 JSON 字符串直接作为模型输入）
        String weeklyBills = billTools.queryBills(
                null, start.format(DATE), end.format(DATE), null, null, 200, ctx);
        String monthOverview = statisticsTools.getMonthlyOverview(null, ctx);
        String categoryDistribution = statisticsTools.getCategoryDistribution(null, null, ctx);
        String trend = statisticsTools.getTrend(6, ctx);
        String plans = planTools.listPlans(0, ctx);

        String userData = """
                统计周期：%s 至 %s

                ## 近7天账单明细与汇总
                %s

                ## 本月收支概览
                %s

                ## 本月支出分类占比
                %s

                ## 近6个月收支趋势
                %s

                ## 持仓中理财计划
                %s
                """.formatted(start.format(DATE), end.format(DATE),
                weeklyBills, monthOverview, categoryDistribution, trend, plans);

        String content = reportChatClient.prompt()
                .system(REPORT_SYSTEM_PROMPT)
                .user(userData)
                .call()
                .content();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("AI 返回内容为空");
        }
        return content.trim();
    }

    /** Markdown → HTML，并包装为邮件友好的内联样式容器 */
    private String wrapEmailHtml(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String body = renderer.render(parser.parse(markdown));

        return """
                <div style="max-width:640px;margin:0 auto;padding:24px;font-family:-apple-system,'PingFang SC','Segoe UI',Roboto,Arial,sans-serif;color:#1f2937;line-height:1.7;font-size:14px;">
                %s
                <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;"/>
                <p style="font-size:12px;color:#9ca3af;margin:0;">本邮件由 FinanceAgent 智能财务助手自动生成，统计周期为最近 7 天。内容仅供参考，不构成投资建议。</p>
                </div>
                """.formatted(body);
    }

    /** 报告标题：每周财务报告（MM.dd-MM.dd） */
    private String buildTitle(LocalDate start, LocalDate end) {
        return "每周财务报告（" + start.format(MD) + "-" + end.format(MD) + "）";
    }

    private ReportPushConfig selectByUserId(Long userId) {
        return configMapper.selectOne(new LambdaQueryWrapper<ReportPushConfig>()
                .eq(ReportPushConfig::getUserId, userId));
    }

    /** 读取配置，不存在则抛业务异常（测试邮件等场景） */
    private ReportPushConfig requireConfig(Long userId) {
        ReportPushConfig config = selectByUserId(userId);
        if (config == null || !StringUtils.hasText(config.getEmail())) {
            throw new BusinessException("请先保存接收邮箱，再发送测试邮件");
        }
        return config;
    }

    private WeeklyReport saveReport(Long userId, String title, String content,
                                    LocalDate start, LocalDate end, int status, String errorMsg) {
        WeeklyReport report = new WeeklyReport();
        report.setUserId(userId);
        report.setTitle(title);
        report.setContent(content);
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setSendStatus(status);
        report.setErrorMsg(errorMsg);
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);
        return report;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
