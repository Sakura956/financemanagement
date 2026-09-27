# 个人财务管理系统-前端

基于 **Vue 3 + TypeScript + Element Plus** 等的全栈个人财务管理系统的前端项目，提供收入支出记录、理财计划管理、多维度统计分析、备忘录、AI 智能助手等功能，并包含完整的管理后台。

**后端项目（Vue 3）**：[https://gitee.com/sakura_cjy/financial-management-api.git](https://gitee.com/sakura_cjy/financial-management-api.git)

## 功能概览

### 用户端
- **收支账单** — 记录每笔收入/支出，支持分类筛选、日期范围查询、金额区间过滤、批量删除
- **理财计划** — 管理投资组合，跟踪本金、当前价值、收益及收益率，支持估值更新与赎回
- **统计分析** — 月度收支总览、分类占比饼图、近 N 个月趋势图、年度汇总报告
- **备忘录** — 待办事项管理，支持提醒时间、完成状态切换
- **AI 助手** — 多会话对话，提供智能财务分析、消费优化建议、理财咨询
- **个人设置** — 修改昵称/头像/密码

### 管理端（ADMIN 角色）
- **管理后台** — 系统概览仪表盘（用户数、账单数、活跃度）
- **用户管理** — 查看所有注册用户及其统计数据，支持封禁/解封
- **分类管理** — 管理收入/支出分类，支持新增、编辑、启用/禁用

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.5 (Composition API + `<script setup>`) |
| 语言 | TypeScript 6.0 |
| 构建工具 | Vite 8 |
| UI 组件库 | Element Plus 2.14 |
| 状态管理 | Pinia 3 |
| 路由 | Vue Router 4 |
| HTTP 客户端 | Axios |
| 图表 | ECharts 6 + vue-echarts |
| CSS 预处理 | Sass |
| 图标 | @element-plus/icons-vue |

## 项目结构

```
FinanceManagementVue/
├── docs/
│   └── API接口详细文档.md          # 后端 API 文档
├── public/
│   └── favicon.ico                  # 网站图标
├── src/
│   ├── api/                         # API 接口层
│   │   ├── request.ts               # Axios 实例、拦截器（鉴权、错误处理）
│   │   └── modules/                 # 按模块拆分的接口
│   │       ├── auth.ts              #   登录/注册/个人信息/改密
│   │       ├── bill.ts              #   账单 CRUD + 分类查询
│   │       ├── financePlan.ts       #   理财计划 CRUD + 估值/状态
│   │       ├── statistics.ts        #   统计（总览/饼图/趋势/年度）
│   │       ├── memo.ts              #   备忘录 CRUD + 切换完成状态
│   │       ├── ai.ts                #   AI 会话/消息
│   │       └── admin.ts             #   管理后台（用户/分类/仪表盘）
│   ├── assets/
│   │   └── styles/
│   │       └── global.scss          # 全局样式 + Element Plus 主题覆盖
│   ├── components/
│   │   └── layout/                  # 布局组件
│   │       ├── AppLayout.vue        #   主布局（sidebar + header + content）
│   │       ├── Sidebar.vue          #   侧边栏导航
│   │       └── AppHeader.vue        #   顶部栏（用户信息/退出）
│   ├── router/
│   │   └── index.ts                 # 路由配置 + 导航守卫（登录/角色校验）
│   ├── stores/
│   │   ├── auth.ts                  # 用户认证状态（token/userInfo/登录/注册）
│   │   └── app.ts                   # 应用状态（侧边栏折叠）
│   ├── types/
│   │   └── index.ts                 # 全局 TypeScript 类型定义
│   ├── utils/
│   │   └── index.ts                 # 工具函数（金额格式化/日期/收益率）
│   ├── views/                       # 页面视图
│   │   ├── auth/                    #   登录 / 注册
│   │   ├── dashboard/               #   首页仪表盘
│   │   ├── bill/                    #   收支账单列表
│   │   ├── financePlan/             #   理财计划
│   │   ├── statistics/              #   统计分析
│   │   ├── memo/                    #   备忘录
│   │   ├── ai/                      #   AI 助手
│   │   ├── profile/                 #   个人设置
│   │   └── admin/                   #   管理后台（用户/分类/仪表盘）
│   ├── App.vue                      # 根组件（启动时恢复用户信息）
│   └── main.ts                      # 入口文件（注册插件/图标/样式）
├── index.html                       # HTML 入口
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
└── .gitignore
```

## 路由表

| 路径 | 页面 | 权限 |
|------|------|------|
| `/login` | 登录 | 公开（已登录自动跳转首页） |
| `/register` | 注册 | 公开 |
| `/dashboard` | 首页仪表盘 | 需登录 |
| `/bills` | 收支账单 | 需登录 |
| `/finance-plans` | 理财计划 | 需登录 |
| `/statistics` | 统计分析 | 需登录 |
| `/memos` | 备忘录 | 需登录 |
| `/ai` | AI 助手 | 需登录 |
| `/profile` | 个人设置 | 需登录 |
| `/admin/dashboard` | 管理后台 | 仅 ADMIN |
| `/admin/users` | 用户管理 | 仅 ADMIN |
| `/admin/categories` | 分类管理 | 仅 ADMIN |

## 环境要求

- **Node.js**: `^20.19.0` 或 `>=22.12.0`
- **npm**: 9.x 及以上（推荐使用 Node 22 LTS）

## 快速开始

```bash
# 1. 克隆项目
git clone https://gitee.com/sakura_cjy/finance-management-vue.git
cd FinanceManagementVue

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

默认开发服务器运行在 `http://localhost:5173`。

## 后端 API

本项目需要配套的**后端服务**提供 API 支持。

项目地址：https://gitee.com/sakura_cjy/financial-management-api.git

- **默认 Base URL**: `http://localhost:8080/api/v1`（可在 `src/api/request.ts` 中修改）
- **鉴权方式**: `Authorization: Bearer {token}`（除登录/注册外的所有接口）
- **API 详细文档**: 见 `docs/API接口详细文档.md`

### 主要 API 模块

| 模块 | 前缀 | 说明 |
|------|------|------|
| 认证 | `/auth/**` | 登录、注册、个人信息、改密 |
| 账单 | `/user/bills/**` | 账单 CRUD、批量删除 |
| 分类 | `/user/categories/**` | 查询可用分类 |
| 理财 | `/user/finance-plans/**` | 计划 CRUD、估值、赎回 |
| 统计 | `/user/statistics/**` | 总览、饼图、趋势、年度汇总 |
| 备忘录 | `/user/memos/**` | 备忘 CRUD、切换完成 |
| AI | `/user/ai/**` | 聊天、会话管理 |
| 管理 | `/admin/**` | 用户管理、分类管理、仪表盘 |

## 可用命令

```bash
# 安装依赖
npm install

# 启动开发服务器（热更新）
npm run dev

# 类型检查
npm run type-check

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## VS Code 推荐配置

安装推荐的插件以获得最佳开发体验：

- [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) — `.vue` 文件语法高亮、类型检查、IntelliSense

请在 VS Code 中禁用 **Vetur** 插件以避免与 Volar 冲突。

## 浏览器扩展

- **Chrome / Edge**: [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
- **Firefox**: [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)

