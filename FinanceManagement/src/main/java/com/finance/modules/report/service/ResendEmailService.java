package com.finance.modules.report.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.finance.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resend 邮件推送服务（HTTP API 直连，无需引入 mail starter）
 *
 * API 文档：POST https://api.resend.com/emails
 * 请求头：Authorization: Bearer {RESEND_API_KEY}
 * 请求体：{ from, to[], subject, html }
 *
 * 说明：
 * - 免费版 100 封/天，个人财务周报场景绰绰有余；
 * - 未验证域名时，from 默认 onboarding@resend.dev，只能发送给
 *   Resend 账号持有者自己的邮箱；验证域名后可发送任意地址。
 */
@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private static final int TIMEOUT_MS = 15_000;

    @Value("${report.push.resend-api-key:}")
    private String apiKey;

    @Value("${report.push.from-email:FinanceAgent <onboarding@resend.dev>}")
    private String fromEmail;

    /**
     * 发送 HTML 邮件
     *
     * @param to      收件邮箱
     * @param subject 邮件主题
     * @param html    HTML 正文
     * @return Resend 返回的邮件ID
     */
    public String send(String to, String subject, String html) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("邮件推送未配置：请在 .env 中设置 RESEND_API_KEY（resend.com 免费注册获取）");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", fromEmail);
        payload.put("to", List.of(to));
        payload.put("subject", subject);
        payload.put("html", html);

        try (HttpResponse response = HttpRequest.post(RESEND_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(payload))
                .timeout(TIMEOUT_MS)
                .execute()) {

            String body = response.body();
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                log.warn("[Report] Resend 发送失败: status={}, body={}", response.getStatus(), body);
                throw new BusinessException("邮件发送失败（HTTP " + response.getStatus() + "）：" + extractMessage(body));
            }
            JSONObject json = JSONUtil.parseObj(body);
            String messageId = json.getStr("id", "");
            log.info("[Report] Resend 邮件已发送: to={}, messageId={}", to, messageId);
            return messageId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Report] Resend 请求异常: to={}", to, e);
            throw new BusinessException("邮件服务连接失败：" + e.getMessage());
        }
    }

    /** 从 Resend 错误响应中提取 message 字段，便于前端展示原因 */
    private String extractMessage(String body) {
        try {
            JSONObject json = JSONUtil.parseObj(body);
            String message = json.getStr("message");
            if (StringUtils.hasText(message)) {
                return message;
            }
            String name = json.getStr("name");
            if (StringUtils.hasText(name)) {
                return name;
            }
        } catch (Exception ignored) {
            // 响应体不是 JSON，原样截断返回
        }
        return StringUtils.hasText(body) && body.length() <= 200 ? body : "请检查 RESEND_API_KEY 与收件邮箱配置";
    }
}
