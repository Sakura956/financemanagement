import request from "@/api/request";
import type { PushConfig, PushConfigRequest, ReportPreview, WeeklyReportItem } from "@/types";

/**
 * 周报邮件推送（Resend）相关接口
 */
export const reportApi = {
  /**
   * 获取当前用户推送配置
   */
  getPushConfig(): Promise<PushConfig> {
    return request.get("/user/report/push-config");
  },

  /**
   * 保存推送配置（邮箱 + 每周开关）
   */
  updatePushConfig(data: PushConfigRequest): Promise<null> {
    return request.put("/user/report/push-config", data);
  },

  /**
   * 发送测试邮件（需已保存邮箱）
   */
  sendTestEmail(): Promise<null> {
    return request.post("/user/report/push-config/test");
  },

  /**
   * 立即生成并发送一份报告（失败返回具体原因）
   * 注：需等待 AI 生成，单独放宽超时（全局默认 30s）
   */
  sendNow(): Promise<null> {
    return request.post("/user/report/send-now", undefined, { timeout: 120000 });
  },

  /**
   * 生成并预览本周报告（不发送、不落库）
   * 注：需等待 AI 生成，单独放宽超时（全局默认 30s）
   */
  previewReport(): Promise<ReportPreview> {
    return request.post("/user/report/preview", undefined, { timeout: 120000 });
  },

  /**
   * 最近 10 份存档报告
   */
  listReports(): Promise<WeeklyReportItem[]> {
    return request.get("/user/report/list");
  },
};
