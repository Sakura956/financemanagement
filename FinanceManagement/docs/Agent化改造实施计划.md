# FinanceManagement Agent化改造 · 分阶段实施计划

> 版本 v1.0 | 制定日期：2026-09-27 | 状态：**待执行**

---

## 〇、项目概述

### 0.1 改造目标

将现有"个人财务管理系统"从 **简单 AI 聊天助手** 升级为 **能理解自然语言、能操作业务数据、能基于用户历史账本做智能分析的 Agent 应用**。

### 0.2 核心能力升级路线

```
当前状态                              目标状态
─────────────────────────────────────────────────────────────────────
用户输入文字  ──→  RestTemplate  ──→  DeepSeek  ──→  返回纯文本回复
                                                          │
                                                          ▼
用户输入文字  ──→  Agent 决策层  ──┬──→  Tool Calling  ──→  读写账单/计划/备忘/统计
                                   │
                                   └──→  RAG 检索增强  ──→  基于历史账本做个性化分析
```

### 0.3 技术选型确认

| 层 | 选型 | 版本 | 理由 |
|---|---|---|---|
| Agent 框架 | **Spring AI** | 1.1.x（稳定版） | Spring Boot 3.4 原生集成，Tool Calling / RAG / 对话记忆开箱即用 |
| 大模型（Chat） | **DeepSeek V4 Pro / Flash** | deepseek-v4-pro / deepseek-flash | 国内访问稳定，性价比高，支持 Tool Calling 和 Reasoning |
| 大模型（Embedding） | **deepseek-embed** | deepseek-embed | DeepSeek 官方 Embedding，中英双语，极便宜 |
| 向量库（开发） | **InMemoryVectorStore** | Spring AI 内置 | 零依赖、快速跑通，重启丢失数据，开发阶段够用 |
| 向量库（生产） | **Milvus / PgVector** | Milvus 2.x | 后续数据量大时切换 |
| 切片策略 | **按业务实体切片** | — | 每条账单/计划/备忘各 = 1 个 Document |

### 0.4 改造范围

| 层 | 涉及模块 | 改动类型 |
|---|---|---|
| 后端 Java | `modules/ai` → 重构为 Agent 入口 | 修改 |
| 后端 Java | 新增 `modules/agent`（核心层） | 新增 |
| 后端 Java | 新增 `modules/agent/tools`（4 个 Tool） | 新增 |
| 后端 Java | 新增 `modules/agent/rag`（RAG 层） | 新增 |
| 后端 Java | `pom.xml` 增加 Spring AI 依赖 | 修改 |
| 后端 Java | `application.yml` 增加 AI 配置 | 修改 |
| 前端 Vue | `views/ai/Chat.vue` 增加工具调用可视化 | 修改 |
| 前端 Vue | `api/modules/ai.ts` 增加 Agent 对话接口 | 修改 |

---

## 一、阶段总览

| 阶段 | 名称 | 核心目标 | 前置依赖 | 预计工时 |
|---|---|---|---|---|
| **阶段 0** | 基础设施准备 | 引入 Spring AI 依赖，跑通 DeepSeek 基础对话 | 无 | 0.5 天 |
| **阶段 1** | Tool Calling 核心 | 将现有 4 个业务 Service 封装为 AI 可调用的 Tool | 阶段 0 | 1 天 |
| **阶段 2** | RAG 知识库搭建 | 向量化用户数据 + 检索增强生成 | 阶段 0 | 1.5 天 |
| **阶段 3** | Agent 编排层 | ChatClient + 多 Tool + RAG + 对话记忆的统一编排 | 阶段 1 + 2 | 1 天 |
| **阶段 4** | 前端改造 | Vue Chat 页面增加工具调用可视化 + 流式响应 | 阶段 3 | 1 天 |
| **阶段 5** | 测试与验收 | 全流程回归测试 + 典型 Agent 场景验收 | 阶段 4 | 0.5 天 |

**总工时：约 5.5 天（可并行：阶段 1 和 阶段 2 可同步推进）**

---

## 二、阶段 0：基础设施准备

### 2.1 目标

- Spring AI 依赖引入成功，项目能正常编译启动
- 打通 DeepSeek Chat API，能通过 Spring AI 的 `ChatClient` 发送请求并获得响应
- 配置文件结构清晰，API Key 通过环境变量注入

### 2.2 涉及文件

| 操作 | 文件路径 |
|---|---|
| **修改** | `FinanceManagement/pom.xml` |
| **修改** | `FinanceManagement/src/main/resources/application.yml` |
| **新增** | `FinanceManagement/src/main/java/com/finance/modules/agent/config/AgentConfig.java` |
| **新增** | `FinanceManagement/src/main/java/com/finance/modules/agent/controller/AgentHealthController.java` |

### 2.3 具体改动

#### pom.xml 新增依赖（示意）

```xml
<!-- Spring AI BOM（版本统一管理） -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.7</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Spring AI + DeepSeek（Chat + Embedding 一个 starter 全搞定） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>

<!-- Spring AI 向量库（开发用内存，生产换 Milvus） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-inmemory</artifactId>
</dependency>
```

#### application.yml 新增配置（示意）

```yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}        # 环境变量注入
      base-url: https://api.deepseek.com
      chat:
        model: deepseek-v4-pro             # 或 deepseek-flash
        options:
          temperature: 0.7
      embedding:
        model: deepseek-embed
    vectorstore:
      inmemory:
        initialize-schema: always          # 开发方便，生产设为 never
```

#### AgentConfig.java 关键骨架（示意）

```java
@Configuration
public class AgentConfig {

    // Spring AI 自动注入 ChatModel（DeepSeek）
    // Spring AI 自动注入 EmbeddingModel（deepseek-embed）
    // Spring AI 自动注入 InMemoryVectorStore

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("你是 FinanceAgent，一个个人财务管理智能助手...")
            .build();
    }
}
```

### 2.4 验收标准

| # | 检查项 | 验证方式 |
|---|---|---|
| 1 | `mvn clean compile` 通过 | 命令行执行编译 |
| 2 | 应用启动无报错 | 控制台无 Spring AI 相关异常 |
| 3 | `/api/agent/health` 返回 OK | curl 或浏览器访问健康检查接口 |
| 4 | 发送测试消息能收到 DeepSeek 回复 | 调用简单 Chat 接口 |

### 2.5 风险点

| 风险 | 应对 |
|---|---|
| Spring AI 版本号不确定 | 执行 `mvn dependency:tree` 确认实际拉取版本，或查 Maven Central |
| DeepSeek API 限流 | 开发阶段用 `deepseek-flash`，配合 Spring AI 的 Retry 机制 |
| 旧 `AiConfig.java`（RestTemplate 版）与 Spring AI 冲突 | 标记为 `@Deprecated`，暂不删除，后续阶段 4 清理 |

---

## 三、阶段 1：Tool Calling 核心

### 3.1 目标

- 将现有 **BillService / StatisticsService / FinancePlanService / MemoService** 的核心方法包装为 Spring AI 的 `@Tool`
- AI Agent 能通过自然语言自动判断何时调用哪个 Tool，并正确传递参数

### 3.2 涉及文件

| 操作 | 文件路径 |
|---|---|
| **新增** | `modules/agent/tools/BillTools.java` |
| **新增** | `modules/agent/tools/StatisticsTools.java` |
| **新增** | `modules/agent/tools/PlanTools.java` |
| **新增** | `modules/agent/tools/MemoTools.java` |
| **新增** | `modules/agent/service/AgentService.java`（接口） |
| **新增** | `modules/agent/service/impl/AgentServiceImpl.java`（实现） |
| **新增** | `modules/agent/controller/AgentChatController.java` |
| **修改** | `AgentConfig.java`（注册 Tools） |

### 3.3 Tool 方法清单

#### BillTools.java（示意）

| 方法 | Tool Description | 用途 |
|---|---|---|
| `queryBills(category, startDate, endDate, keyword)` | 查询用户账单记录，支持分类/日期范围/关键词筛选 | "我这个月餐饮花了多少" |
| `createBill(description, amount, category, date)` | 记录一笔新账单，自动识别日期和分类 | "昨天沙县小吃28块" |
| `deleteBill(billId)` | 删除一笔账单 | "帮我删掉那条错误的账单" |

#### StatisticsTools.java（示意）

| 方法 | Tool Description | 用途 |
|---|---|---|
| `getMonthlyOverview(yearMonth)` | 获取某月收支概览（收入/支出/结余） | "我上个月结余多少" |
| `getCategoryPie(yearMonth)` | 获取某月分类支出占比饼图数据 | "这个月哪类花最多" |
| `getTrend(months)` | 获取近 N 个月收支趋势 | "最近半年支出趋势怎么样" |

#### PlanTools.java（示意）

| 方法 | Tool Description | 用途 |
|---|---|---|
| `listPlans(status)` | 列出用户所有财务计划（持有中/已完成） | "我有哪些理财计划" |
| `createPlan(name, targetAmount, category, description)` | 创建一个财务计划或预算 | "帮我设每月餐饮预算1500" |
| `valuation(planId)` | 计算指定理财计划的当前估值和收益率 | "我的XX基金赚了多少" |

#### MemoTools.java（示意）

| 方法 | Tool Description | 用途 |
|---|---|---|
| `listMemos(keyword)` | 列出用户备忘录，支持关键词搜索 | "我记了什么关于房租的事" |
| `createMemo(title, content)` | 创建一条备忘录 | "把房东说涨租5%记一下" |

### 3.4 代码骨架（示意，非完整实现）

```java
// BillTools.java — 关键骨架
@Component
public class BillTools {

    private final BillService billService;
    private final SecurityUtil securityUtil;

    @Tool(description = "查询用户的账单记录，支持按分类、日期范围、关键词筛选。" +
            "参数：category（支出/餐饮/交通等，可null），startDate（yyyy-MM-dd，可null），" +
            "endDate（yyyy-MM-dd，可null），keyword（模糊匹配备注，可null）")
    public String queryBills(
            @ToolParam(description = "账单分类名称，如'餐饮'、'交通'") String category,
            @ToolParam(description = "开始日期，格式yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式yyyy-MM-dd") String endDate,
            @ToolParam(description = "关键词，匹配账单备注") String keyword) {
        // 调用现有 BillService 做查询
        // 结果转 JSON 返回给 AI
    }

    @Tool(description = "记录一笔新账单/支出。" +
            "参数：description（备注说明），amount（金额，正数），" +
            "category（分类，如'餐饮'、'交通'），date（yyyy-MM-dd，可null默认今天）")
    public String createBill(String description, BigDecimal amount, String category, String date) {
        // 调用现有 BillService 创建账单
        // 返回操作结果
    }
}
```

### 3.5 Tool 注册（AgentConfig 中）

```java
@Bean
public ChatClient chatClient(ChatModel chatModel, BillTools billTools, 
                              StatisticsTools statsTools, PlanTools planTools, MemoTools memoTools) {
    return ChatClient.builder(chatModel)
        .defaultTools(billTools, statsTools, planTools, memoTools)   // 注册所有 Tools
        .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemoryRepository, userId))  // 对话记忆
        .build();
}
```

### 3.6 验收标准

| # | 检查项 | 验证方式 |
|---|---|---|
| 1 | 发送"我这个月餐饮花了多少"能正确调用 `queryBills` | 观察日志中 Tool 调用记录 |
| 2 | 发送"昨天沙县小吃28块"能正确创建账单 | 查数据库确认新记录存在 |
| 3 | 发送"帮我设每月餐饮预算1500"能创建 Plan | 查数据库确认新计划存在 |
| 4 | 发送"把房东说涨租5%记一下"能创建 Memo | 查数据库确认新备忘存在 |
| 5 | 工具调用参数正确，无类型转换错误 | 检查 JSON 返回体 |

### 3.7 注意事项

- **用户隔离**：每个 Tool 方法内部需通过 `SecurityUtil.getCurrentUserId()` 获取当前用户 ID，确保不能跨用户读写数据
- **Tool Description 写详细**：AI 靠 description 决定调用哪个 Tool，描述越精准，调用越准确
- **参数类型校验**：用 `@ToolParam` 标注参数含义，避免 AI 传错类型（如金额传字符串）

---

## 四、阶段 2：RAG 知识库搭建

### 4.1 目标

- 将用户历史账单、财务计划、备忘录向量化存入 InMemoryVectorStore
- 用户提问时，先从向量库检索相关上下文，再结合 Tool Calling 生成回答

### 4.2 涉及文件

| 操作 | 文件路径 |
|---|---|
| **新增** | `modules/agent/rag/RagService.java`（核心服务） |
| **新增** | `modules/agent/rag/ChunkingService.java`（切片服务） |
| **新增** | `modules/agent/rag/VectorIndexRunner.java`（启动时自动索引） |
| **新增** | `modules/agent/dto/RagQueryRequest.java` |
| **修改** | `AgentConfig.java`（注入 EmbeddingModel 和 VectorStore） |
| **修改** | `AgentChatController.java`（增加 RAG 检索 + 增强生成逻辑） |

### 4.3 切片策略

```
数据源           切片单位                    Embedding 输入文本格式                          Metadata
─────────────────────────────────────────────────────────────────────────────────────────────────────
BillRecord   →  每条账单 = 1 Document     "账单记录 | 日期: xxx | 分类: xxx | 金额: xxx元        {type:"bill", billId, userId, category, date}
                                               备注: xxx"

FinancePlan  →  每个计划 = 1 Document     "财务计划 | 名称: xxx | 周期: xxx | 预算: xxx元         {type:"plan", planId, userId, category, budget}
                                               已使用: xxx | 说明: xxx"

Memo         →  每条备忘 = 1 Document     "备忘录 | 日期: xxx | 标题: xxx                         {type:"memo", memoId, userId, date}
                                               内容: xxx"
                                               （长备忘>500字时，按段落+滑动窗口二次切分）
```

### 4.4 代码骨架（示意）

```java
// ChunkingService.java — 业务实体切片
@Service
public class ChunkingService {

    public List<Document> chunkBills(List<BillRecord> bills) {
        return bills.stream().map(bill -> {
            String content = String.format(
                "账单记录 | 日期: %s | 分类: %s | 金额: %.2f元\n备注: %s",
                bill.getDate(), bill.getCategory(), bill.getAmount(), bill.getDescription());
            return Document.builder()
                .withContent(content)
                .withMetadata("type", "bill")
                .withMetadata("billId", bill.getId())
                .withMetadata("userId", bill.getUserId())
                .withMetadata("category", bill.getCategory())
                .withMetadata("date", bill.getDate().toString())
                .build();
        }).toList();
    }

    // chunkPlans()、chunkMemos() 同理...
}

// RagService.java — 检索 + 增强生成
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChunkingService chunkingService;
    private final BillMapper billMapper;
    // ...其他 Mapper

    // 全量重建用户向量索引
    public void rebuildUserIndex(Long userId) {
        List<Document> docs = new ArrayList<>();
        docs.addAll(chunkingService.chunkBills(billMapper.selectByUserId(userId)));
        docs.addAll(chunkingService.chunkPlans(planMapper.selectByUserId(userId)));
        docs.addAll(chunkingService.chunkMemos(memoMapper.selectByUserId(userId)));
        vectorStore.add(docs);
    }

    // 检索：按 userId 过滤 + 向量相似度搜索
    public List<Document> search(Long userId, String question, int topK) {
        Filter filter = Filter.builder()
            .and(new FilterExpressionBuilder().eq("userId", userId).build())
            .build();
        return vectorStore.similaritySearch(
            SearchRequest.builder().query(question).filter(filter).topK(topK).build()
        );
    }
}
```

### 4.5 Agent 集成 RAG 的 Prompt 模板

```java
// 在 AgentServiceImpl 中，RAG 检索结果注入 Prompt
String systemPrompt = """
    你是 FinanceAgent，个人财务管理智能助手。
    以下是用户的相关财务数据（来自知识库检索）：
    %s

    基于这些数据和你可用的工具，回答用户的问题。
    如果知识库中没有相关数据，调用对应工具去实时查询。
    """.formatted(retrievedContext);
```

### 4.6 验收标准

| # | 检查项 | 验证方式 |
|---|---|---|
| 1 | 应用启动后能看到向量索引构建日志 | 控制台输出 "Indexed N documents for user X" |
| 2 | 向量库中有预期数量的 Document | 调用 debug 接口检查 InMemoryVectorStore.size() |
| 3 | "我去年春节花了多少" 能检索到春节相关账单 | 检查日志中的检索结果 |
| 4 | "那个涨租5%的事" 能检索到对应备忘 | 同上 |
| 5 | 检索结果的 metadata 过滤只返回当前用户数据 | 故意传 userId 对比返回结果 |

### 4.7 风险点

| 风险 | 应对 |
|---|---|
| 向量库全量重建耗时 | 用 `@Async` 异步执行，加进度日志；用户量大时改为增量更新 |
| Embedding API 限流（deepseek-embed 20万 token/min） | 批量调用 + 间隔 sleep，或用本地 Ollama（BGE-M3）做 Embedding 替代 |
| 检索效果不好 | 调 topK、调 Embedding 模型、增加 Metadata 过滤 |

---

## 五、阶段 3：Agent 编排层

### 5.1 目标

- 将 ChatClient + Tool Calling + RAG + 对话记忆 串联为一个统一的 Agent 服务
- 正确处理多轮对话上下文
- 提供 SSE 流式响应接口

### 5.2 涉及文件

| 操作 | 文件路径 |
|---|---|
| **修改** | `AgentService.java`（增加 RAG + 记忆 + 流式方法） |
| **修改** | `AgentServiceImpl.java`（编排逻辑） |
| **修改** | `AgentChatController.java`（新增 SSE 接口） |
| **新增** | `modules/agent/memory/ConversationMemoryService.java` |
| **新增** | `modules/agent/dto/AgentChatRequest.java` |
| **新增** | `modules/agent/dto/AgentChatResponse.java` |

### 5.3 Agent 处理流程

```
用户输入 "帮我看看上个月餐饮预算花了多少"
        │
        ▼
  ① 对话记忆加载  →  ChatMemoryAdvisor 注入历史上下文
        │
        ▼
  ② RAG 检索      →  RagService.search(userId, question, topK=5)
        │
        ▼
  ③ Prompt 组装   →  System Prompt + RAG 上下文 + 对话历史 + 用户输入
        │
        ▼
  ④ LLM 推理      →  ChatClient.call() / stream()
        │
        ├── LLM 返回 Tool Call 请求？ ──→ 自动执行 Tool ──→ Tool 结果回注 LLM ──→ 回到 ④
        │
        └── LLM 返回最终文本？ ──→ 流式/一次性返回给用户
        │
        ▼
  ⑤ 对话记忆保存  →  ChatMemoryAdvisor 持久化到 Redis
```

### 5.4 代码骨架（示意）

```java
// AgentServiceImpl.java — 编排核心
@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final ConversationMemoryService memoryService;

    @Override
    public ChatResponse chat(Long userId, String sessionId, String message) {
        // 1. RAG 检索
        List<Document> ragDocs = ragService.search(userId, message, 5);
        String ragContext = ragDocs.stream()
            .map(d -> "[" + d.getMetadata().get("type") + "] " + d.getContent())
            .collect(Collectors.joining("\n---\n"));

        // 2. 构建带 RAG 上下文的 ChatClient（带用户隔离的对话记忆）
        ChatClient userClient = chatClient.mutate()
            .defaultSystem(BASE_SYSTEM_PROMPT + "\n\n知识库检索结果：\n" + ragContext)
            .defaultAdvisors(a -> a.param(CONVERSATION_ID, sessionId))   // 对话隔离
            .build();

        // 3. 非流式调用
        String response = userClient.prompt()
            .user(message)
            .call()
            .content();

        return new ChatResponse(response);
    }

    @Override
    public Flux<String> chatStream(Long userId, String sessionId, String message) {
        // 流式版本，同理
        return chatClient.mutate()
            .defaultSystem(BASE_SYSTEM_PROMPT + "\n\n知识库检索结果：...")
            .defaultAdvisors(a -> a.param(CONVERSATION_ID, sessionId))
            .build()
            .prompt()
            .user(message)
            .stream()
            .content();
    }
}
```

### 5.5 验收标准

| # | 检查项 | 验证方式 |
|---|---|---|
| 1 | 多轮对话上下文正确 | 先问"我这个月餐饮花了多少"，再问"那上个月呢"，第二轮应理解"上个月"指代 |
| 2 | Tool + RAG 联合工作 | "我去年春节餐饮花了多少，对比今年怎么样" → 先 RAG 检索，再 Tool 调用汇总 |
| 3 | SSE 流式响应正常 | 前端收到逐步输出的 token |
| 4 | 对话记忆按 sessionId 隔离 | 不同会话互不干扰 |
| 5 | 长对话（>10轮）不丢失上下文 | 持续对话观察是否遗忘 |

---

## 六、阶段 4：前端改造

### 6.1 目标

- Vue 3 Chat 页面支持 Agent 对话（含 SSE 流式响应）
- 工具调用过程可视化（让用户看到 AI 正在"查账单""记一笔"）
- 保持现有 UI 风格，不影响 UniApp 移动端（本次只改 Vue 端）

### 6.2 涉及文件

| 操作 | 文件路径 |
|---|---|
| **修改** | `FinanceManagementVue/src/views/ai/Chat.vue` |
| **修改** | `FinanceManagementVue/src/api/modules/ai.ts` |
| **修改** | `FinanceManagementVue/src/types/index.ts`（新增 Agent 相关类型） |
| **新增** | `FinanceManagementVue/src/components/ai/ToolCallBadge.vue`（工具调用徽章组件） |
| **新增** | `FinanceManagementVue/src/components/ai/StreamMessage.vue`（流式消息组件） |

### 6.3 UI 变化

```
改造前                              改造后
─────────────────────────────────────────────────────
┌──────────────────────────┐       ┌──────────────────────────┐
│ 💬 聊天助手               │       │ 🤖 FinanceAgent           │
│                          │       │                          │
│ 用户: 我这个月餐饮花多少  │       │ 用户: 我这个月餐饮花多少  │
│                          │       │                          │
│ AI: 你本月餐饮共花了      │       │ 🤖 正在查询账单数据...    │  ← 新增：Tool 调用状态
│   2145元，比上月多15%     │       │ [查询账单] ✓ 完成          │  ← 新增：Tool 调用徽章
│                          │       │                          │
│ [输入框] [发送]          │       │ AI: 你本月餐饮共花了      │
└──────────────────────────┘       │   2145元，比上月多15%     │
                                   │ ┌─ 来源 ─────────────┐    │  ← 新增：RAG 溯源
                                   │ │ 2026-09-15 外婆家  │    │
                                   │ │ 2026-09-20 海底捞  │    │
                                   │ └────────────────────┘    │
                                   │                          │
                                   │ [输入框] [发送]           │
                                   └──────────────────────────┘
```

### 6.4 关键改动（示意）

#### ai.ts — 新增 Agent API

```typescript
// 原有简单聊天保留
export function chat(data: ChatRequest) { ... }

// 新增：Agent 对话（SSE 流式）
export function chatStream(data: AgentChatRequest) {
  // 返回 EventSource / fetch 的 ReadableStream
}
```

#### Chat.vue — 处理 Tool Call 可视化

```vue
<template>
  <div class="message" v-for="msg in messages" :key="msg.id">
    <!-- AI 消息：显示 Tool 调用链 -->
    <div v-if="msg.role === 'assistant'">
      <!-- Tool 调用徽章 -->
      <div v-for="tool in msg.toolCalls" :key="tool.id" class="tool-badge">
        <ToolCallBadge :tool="tool" :status="tool.status" />
      </div>
      <!-- 最终回复 -->
      <StreamMessage :content="msg.content" :sources="msg.sources" />
    </div>
  </div>
</template>
```

### 6.5 验收标准

| # | 检查项 | 验证方式 |
|---|---|---|
| 1 | 流式打字机效果 | 浏览器打开 Chat 页面，回复逐字出现 |
| 2 | Tool 调用徽章显示 | AI 调用 Tool 时出现对应徽章（如 "查询账单"），带 loading → success 动画 |
| 3 | RAG 溯源显示 | 回复下方显示参考来源（账单日期/备忘标题） |
| 4 | 原有功能不退化 | 旧的简单聊天接口仍能正常工作 |
| 5 | 移动端（UniApp）不受影响 | UniApp 端 AI 页面仍正常（本次不改） |

---

## 七、阶段 5：测试与验收

### 7.1 目标

全流程回归测试 + 典型 Agent 场景验收，确保改造不破坏原有业务。

### 7.2 典型测试场景

| # | 场景 | 用户输入 | 预期行为 |
|---|---|---|---|
| 1 | Tool Calling · 查账单 | "我这个月餐饮花了多少" | 调用 `queryBills` → 返回汇总数字 |
| 2 | Tool Calling · 记一笔 | "昨天沙县小吃28块" | 调用 `createBill` → 数据库新增记录 |
| 3 | Tool Calling · 设预算 | "帮我设每月餐饮预算1500" | 调用 `createPlan` → 数据库新增计划 |
| 4 | RAG · 历史检索 | "我去年春节花了多少" | 先向量检索春节相关账单 → 调用汇总 Tool |
| 5 | 多轮对话 · 上下文 | Q1:"我这个月结余多少" Q2:"那上个月呢" | Q2 正确理解 "上个月" |
| 6 | Tool + RAG 联合 | "我今年春节和去年春节花的对比" | RAG 检索 + Statistics Tool 汇总 |
| 7 | 用户隔离 | 换账号提问 | 不能看到其他用户数据 |
| 8 | 流式响应 | 任意问题 | 打字机效果，不阻塞 UI |
| 9 | 原有功能回归 | 直接调 BillController / StatisticsController | 无报错，数据读写正常 |

### 7.3 验收方式

- 手动逐步验证上述场景
- 检查后端日志：Tool 调用记录、RAG 检索日志、Embedding 调用统计
- 抽查数据库：确认新增数据正确
- 抽查 Redis：确认对话记忆持久化

---

## 八、项目目录最终结构

```
com/finance/
├── modules/
│   ├── agent/                          ★ 新增模块
│   │   ├── config/
│   │   │   └── AgentConfig.java        (新增)
│   │   ├── controller/
│   │   │   ├── AgentHealthController.java   (新增)
│   │   │   └── AgentChatController.java     (新增，或替代旧AiChatController)
│   │   ├── dto/
│   │   │   ├── AgentChatRequest.java   (新增)
│   │   │   ├── AgentChatResponse.java  (新增)
│   │   │   └── RagQueryRequest.java    (新增)
│   │   ├── memory/
│   │   │   └── ConversationMemoryService.java  (新增)
│   │   ├── rag/
│   │   │   ├── ChunkingService.java    (新增)
│   │   │   ├── RagService.java         (新增)
│   │   │   └── VectorIndexRunner.java   (新增)
│   │   ├── service/
│   │   │   ├── AgentService.java        (新增)
│   │   │   └── impl/
│   │   │       └── AgentServiceImpl.java  (新增)
│   │   └── tools/
│   │       ├── BillTools.java           (新增)
│   │       ├── MemoTools.java           (新增)
│   │       ├── PlanTools.java           (新增)
│   │       └── StatisticsTools.java     (新增)
│   ├── ai/                             ★ 旧模块，标记废弃或重构
│   │   ├── config/AiConfig.java         (标记 @Deprecated)
│   │   ├── controller/AiChatController.java  (后续删除，迁移到 agent)
│   │   └── ...
│   ├── bill/                           (不变)
│   ├── plan/                           (不变)
│   ├── memo/                           (不变)
│   └── statistics/                     (不变)
├── common/                             (不变)
├── security/                           (不变)
└── util/                               (不变)
```

---

## 九、风险与注意事项汇总

| # | 风险 | 影响阶段 | 应对策略 |
|---|---|---|---|
| 1 | Spring AI 版本兼容性问题 | 阶段 0 | 选稳定版 1.1.7，而非 2.0.x 开发版；遇到类找不到立即查 Spring AI GitHub issue |
| 2 | DeepSeek Embedding API 限流 | 阶段 2 | 批量调用+间隔；备选 Ollama 本地 BGE-M3 |
| 3 | Tool Description 写得不好导致 Agent 调错 Tool | 阶段 1 | 反复迭代优化 description；实际测试观察 Tool 选择是否正确 |
| 4 | RAG 检索效果差（返回无关数据） | 阶段 2 | 调 topK、增加 Metadata 过滤、检查切片质量 |
| 5 | 旧 AiConfig 与 Spring AI 冲突 | 阶段 0 | 标记 @Deprecated，不立即删除；AgentChatController 独立新建 |
| 6 | 用户数据隔离不严 | 阶段 1 | 每个 Tool 方法开头强制从 SecurityUtil 取 userId，绝不信任前端传参 |
| 7 | 前端 SSE 跨域/代理问题 | 阶段 4 | Vite dev server 配 proxy，生产 Nginx 配 SSE 转发头 |
| 8 | InMemoryVectorStore 重启丢失数据 | 阶段 2 | 每次启动自动重建索引（VectorIndexRunner）；生产换持久化向量库 |

---

## 十、后续演进路线（本计划之外）

完成上述 5 个阶段后，可考虑：

| 方向 | 说明 | 优先级 |
|---|---|---|
| **多 Agent 协作** | 记账 Agent + 分析 Agent + 提醒 Agent 分工协作 | 中 |
| **定时任务 Agent** | 每天自动分析支出、每周生成报告、月底提醒账单 | 高 |
| **移动端适配** | UniApp 增加 Agent 对话和工具调用可视化 | 高 |
| **生产级向量库** | PgVector / Milvus，持久化 + 高性能检索 | 中 |
| **本地 Embedding** | Ollama + BGE-M3，省 API 费、数据不出网 | 中 |
| **MCP 协议支持** | 让 Agent 能调用外部 MCP Server 的工具 | 低 |
| **语音交互** | ASR + TTS，让 Agent 能听能说 | 低 |

---

*文档结束 · 执行前请确认以上内容*
