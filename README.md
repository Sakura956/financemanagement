# 个人财务管理系统 FinanceManagement

集记账、理财、统计、备忘与 **AI Agent 智能顾问**于一体的全栈个人财务管理平台。

后端基于 Spring Boot 3 提供 RESTful API，前端基于 Vue 3 + TypeScript 构建单页应用，另有 uni-app 移动端。AI 助手基于 Spring AI 完成 Agent 化改造：大模型可在对话中**直接操作你的账单、统计、理财计划与备忘录**（如"帮我记一笔午餐 25 元"），回复流式呈现、工具调用过程实时可视化。

## 核心特性

### 基础业务

- **收支记账** — 收入/支出双类型、多条件筛选（分类/日期/关键词/金额区间）、批量删除、汇总统计
- **统计分析** — 月度概览、分类占比饼图、收支趋势折线图、年度汇总（ECharts 可视化）
- **理财计划** — 投资计划全生命周期追踪（持有中 → 已赎回），自动计算收益金额与收益率、投资组合汇总
- **备忘录** — 待办管理、提醒时间、完成状态切换、关键词搜索
- **管理后台** — 用户封禁/解封、账单分类管理、运营数据看板（ADMIN 角色）
- **安全认证** — Spring Security 无状态 JWT + Redis Token 二次校验、双端登录互踢、接口级数据隔离

### AI Agent 智能助手（亮点）

基于 **Spring AI 1.1.7** 的完整 Agent 实现：

- **工具调用（Tool Calling）** — 内置 11 个业务工具，模型按需自动选择调用，直接读写真实业务数据
- **RAG 检索增强** — 账单/理财计划/备忘录自动切片入向量库（Embedding：BAAI/bge-m3），回答附带参考来源溯源
- **流式对话（SSE）** — 打字机效果逐 token 输出，工具调用以徽章实时展示（执行中/成功/失败）
- **多会话记忆** — Redis 滑动窗口记忆（供模型消费）+ MySQL 完整历史（供前端展示）双写
- **架构解耦** — userId 通过 ToolContext 注入（解决 Reactor 线程 ThreadLocal 丢失），向量库不可用时自动降级不影响对话

## 技术栈

**后端**：Java 17 · Spring Boot 3.4.4 · Spring Security 6 · Spring AI 1.1.7 · MyBatis-Plus 3.5.8 · MySQL 8 · Redis 7 · JJWT · Hutool

**前端**：Vue 3.5 · TypeScript · Vite · Vue Router · Pinia · Element Plus · ECharts · Axios · Sass

**模型**：DeepSeek V3.2（对话，官方 API，支持 Tool Calling）+ BAAI/bge-m3（Embedding，SiliconFlow）

## 仓库结构

```
FinanceManagement/
├── FinanceManagement/            # 后端（Spring Boot 3 + Spring AI Agent）
│   ├── src/main/java/com/finance/
│   │   ├── modules/auth/         #   认证（注册/登录/JWT）
│   │   ├── modules/bill/         #   账单
│   │   ├── modules/statistics/   #   统计分析
│   │   ├── modules/plan/         #   理财计划
│   │   ├── modules/memo/         #   备忘录
│   │   ├── modules/agent/        #   AI Agent（工具/RAG/记忆/SSE 编排）
│   │   ├── modules/ai/           #   旧版 AI 助手（UniApp 兼容，已 @Deprecated）
│   │   └── modules/admin/        #   管理端
│   ├── docs/                     #   API 接口文档、Agent 实施计划等
│   └── README.md
├── FinanceManagementVue/         # Web 前端（Vue 3 + TypeScript + Element Plus）
└── FinanceManagementUniApp/      # 移动端（uni-app）
```

## 快速开始

### 环境要求

- JDK 17+、Node.js 18+
- MySQL 8.x、Redis 7.x
- 大模型 API Key：[DeepSeek 官方](https://platform.deepseek.com)（对话）+ [SiliconFlow](https://siliconflow.cn)（Embedding，需实名认证）

### 1. 启动后端

```bash
cd FinanceManagement

# 在 FinanceManagement/ 下创建 .env 文件，填入：
#   DB_PASSWORD=你的MySQL密码
#   JWT_SECRET=至少256位随机字符串
#   AI_API_KEY=DeepSeek官方Key
#   SILICONFLOW_API_KEY=SiliconFlow Key

mvnw.cmd spring-boot:run        # 或 IDE 直接运行启动类
```

后端默认运行在 `http://localhost:8080`。

### 2. 启动前端

```bash
cd FinanceManagementVue
npm install
npm run dev
```

访问 `http://localhost:5173`，注册账号即可体验全部功能。

## 文档

- [后端 README](FinanceManagement/README.md) — 模块说明、配置参数、项目结构
- [前端 README](FinanceManagementVue/README.md) — 页面功能、前端架构
- [API 接口详细文档](FinanceManagement/docs/API接口详细文档.md) — 全部接口 + Agent SSE 事件协议
- [Agent 化改造实施计划](FinanceManagement/docs/Agent化改造实施计划.md) — Agent 模块设计文档
- [项目总文档](个人财务管理项目文档.md) — 项目整体介绍与技术细节
