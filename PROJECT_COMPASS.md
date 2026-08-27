# 🧭 PROJECT COMPASS —— 项目北极星 & 面试冲刺指南

> **这是本项目的"初衷文档"。未来的每一次工作，先读这里，再动手。别跑偏，别忘本。**
> 配套的技术速查见 `CLAUDE.md`；本文件负责"为什么做、怎么协作、学到哪了"。

---

## 0. 我是谁 · 我要什么（初衷 · 不可动摇）

我（项目主人）是一名**正在求职的候选人**。这个 SuperBizAgent 项目会作为**简历里的核心项目**，我希望它成为**求职的强力加分项**。

因此，三个目标，按优先级排列：

| 优先级 | 目标 | 判定标准 |
|-------|------|---------|
| **P0** | **彻底吃透项目**，任何方面任何细节都能讲清楚，能扛住面试官刁钻、咄咄逼人的追问 | 我能脱稿讲清每个模块的"是什么/为什么这么设计/换个方案会怎样/坑在哪" |
| **P1** | **找出 Bug 与隐患**：先讲清楚是什么问题、为什么是问题，再决定是否修 | 有一份持续更新的 Bug 清单（见第 5 节），每条都能解释 |
| **P2** | **给出可完整运行项目的环境**：依赖、版本、启动顺序、mock/真实两种玩法 | 一个新人照着第 6 节能把项目跑起来 |

---

## 1. 协作约定（Claude 每次都要遵守）

1. **以"讲懂我"为第一目标**。不要只给结论，要给"为什么"、"背后的原理"、"面试官可能怎么追问"。宁可啰嗦，不要跳步。
2. **面试视角**：解释任何点时，顺手想一句"如果面试官在这里追问，会问什么？"，并把问题和答案沉淀到第 4 节。
3. **发现 Bug：先报告，后动手**。说清楚①现象②根因③影响④修法，得到我确认后再改代码。不要静默修改。
4. **环境要给全**：涉及运行时，务必写清版本、端口、依赖服务、环境变量、启动先后顺序。
5. **诚实**：不确定的地方要说"这里我需要再确认代码"，不能编。代码里的注释可能是错的/过时的，以代码实际行为为准。
6. **中文交流**（本项目注释与文档均为中文）。

---

## 2. 一句话讲清这个项目（电梯陈述草稿）

> SuperBizAgent 是一个基于 **Spring Boot 3.2 + Spring AI Alibaba（阿里 DashScope/通义千问）** 的企业级智能体应用，对外用一个 9900 端口的 HTTP 服务，内部集成两大能力：
> ① **RAG 智能问答**——文档上传后切片、向量化存进 **Milvus**，用 `ReactAgent` 自动调工具（内部文档检索、Prometheus 告警、时间、腾讯云日志）做多轮、流式（SSE）问答；
> ② **AIOps 智能运维**——用 **Supervisor → Planner/Replanner + Executor** 多智能体协作，自动读告警、查日志、按固定模板产出《告警分析报告》。
> 亮点：多智能体编排、RAG、MCP 工具接入、mock/真实双模式、SSE 流式输出。

> **待打磨**：把这段练到能脱稿 30 秒说完，并准备好每一句被追问后的展开。

---

## 3. 深度理解进度追踪（活表 · 学一块勾一块）

> 状态：⬜ 未开始 / 🟨 讲了但没吃透 / ✅ 能扛追问。每块都要能用"我自己的话"复述。

| # | 主题 | 关键文件 | 状态 | 我的一句话复述（自己填/校对） |
|---|------|---------|------|------------------------------|
| 1 | 整体架构 & 请求流转 | `ChatController` | ⬜ | |
| 2 | ReactAgent 聊天链路（chat / chat_stream） | `ChatService` `ChatController` | ⬜ | |
| 3 | AIOps 多智能体编排（Supervisor/Planner/Executor） | `AiOpsService` | ⬜ | |
| 4 | 两类工具：本地 @Tool vs MCP | `agent/tool/*` `ChatService` | ⬜ | |
| 5 | mock 模式 vs 真实模式的切换机制 | `application.yml` `QueryMetricsTools` `QueryLogsTools` | ⬜ | |
| 6 | RAG 全链路：上传→切片→向量化→检索 | `FileUploadController` `VectorIndexService` `DocumentChunkService` `VectorEmbeddingService` `VectorSearchService` | ⬜ | |
| 7 | Milvus 集合/维度/索引设计 | `MilvusConstants` `MilvusConfig` `MilvusClientFactory` | ⬜ | |
| 8 | 会话管理 & 滑动窗口 & 线程安全 | `ChatController.SessionInfo` | ⬜ | |
| 9 | SSE 流式输出原理 | `ChatController` | ⬜ | |
| 10 | DashScope 两条接入路径 | `ChatService` `VectorEmbeddingService` `RagService` | ⬜ | |
| 11 | Prompt 工程（三个 Agent 的系统提示词） | `AiOpsService` | ⬜ | |
| 12 | 依赖与版本（Spring AI Alibaba BOM 等） | `pom.xml` | ⬜ | |

---

## 4. 面试问答演练库（活库 · 持续补充）

> 每深入一块，就把"面试官可能怎么问 + 我怎么答"记在这里。下面是种子问题，答案随理解加深逐步充实。

### 架构类
- **Q：为什么聊天和运维要分两套 Agent，而不是一个大 Agent？** —— 待补：职责边界、Prompt 复杂度、可控性、报告模板强约束。
- **Q：一次 `/api/chat_stream` 请求从进来到吐字，中间发生了什么？** —— 待补：参数校验 → 取/建 session → 建 DashScopeApi/ChatModel → 拼历史进 systemPrompt → 建 ReactAgent → `agent.stream()` → 订阅 `StreamingOutput` 按 `OutputType` 分发 → SSE 推 `content`/`done`。

### Agent / 多智能体类
- **Q：Planner 和 Replanner 是两个 Agent 吗？** —— 待补：不是，同一个 `planner_agent` 兼任，靠 Prompt 里的 `decision(PLAN|EXECUTE|FINISH)` 状态机驱动。
- **Q：最终报告是从哪拿到的？** —— 待补：从图状态 `planner_plan`（`AssistantMessage`）里取，不是 Supervisor 的返回文本。见 `extractFinalReport`。
- **Q：怎么防止大模型编造数据？** —— 待补：Prompt 里"严禁编造 + 同一工具连续失败 3 次就停并如实说明"。

### RAG / 向量类
- **Q：为什么向量维度是 1024？改用别的 embedding 模型要动哪里？** —— 待补：`text-embedding-v4` 输出 1024 维；改模型要同步 `MilvusConstants.VECTOR_DIM` 和集合 schema，否则插入/检索维度不匹配。
- **Q：切片为什么要 overlap（重叠 100 字）？** —— 待补：避免答案被切在两片交界处丢失语义。
- **Q：检索到的文档是怎么喂给模型的？** —— 待补：注意实际聊天走的是 `InternalDocsTools` 工具检索，不是 `RagService`（见 CLAUDE.md 说明，`RagService` 未接线）。

### 工程 / 并发类
- **Q：会话存在哪？重启会怎样？为什么用 ReentrantLock？** —— 待补：`ConcurrentHashMap` 内存态，重启即失；`SessionInfo` 内部用锁保证读写历史的原子性、返回副本防并发修改。
- **Q：SSE 的 `SseEmitter` 超时设了多少？为什么运维接口是 10 分钟？** —— 待补：chat 5min、ai_ops 10min，告警分析多轮调用慢。

### 刁钻 / 压力类（面试官挑刺）
- **Q：你这个 mock/真实模式切换，真的切干净了吗？** —— ⚠️ 见第 5 节 Bug #1，**这是个真实缺陷**，要么老实承认并说清修法（加分！），要么先修掉。
- **Q：`RagService` 你写了但没用上？** —— 待补：如实说明它是独立/遗留的 RAG 实现，线上聊天走 ReactAgent；能讲清"为什么留着/两条路的差异"反而加分。

---

## 5. Bug & 隐患追踪清单（活清单 · 先报告后修）

> 规则：每条写清【现象】【根因】【影响】【建议修法】【状态：待确认/待修/已修/暂不修】。

| # | 严重度 | 标题 | 状态 |
|---|-------|------|------|
| 1 | 中 | 真实模式下 `QueryLogsTools` 未被排除 | 待确认（已告知，待你决定是否修） |

### Bug #1：真实模式下 `QueryLogsTools` 仍然被注册和注入
- **现象**：`cls.mock-enabled=false`（真实模式）时，本意是"日志查询交给腾讯云 MCP 工具，本地 `QueryLogsTools` 不参与"。但实际本地工具依然存在于 Agent 的工具列表里。
- **根因**：`QueryLogsTools` 类上只有 `@Component`，**没有 `@ConditionalOnProperty`**。Spring 无条件创建这个 bean，于是 `ChatService`/`AiOpsService` 里 `@Autowired(required=false)` 的 `queryLogsTools` 永远非 null，`buildMethodToolsArray()` 永远把它塞进工具数组。代码注释"真实模式不包含 QueryLogsTools"没有真正生效。
- **影响**：真实模式下 Agent 同时看到本地 `queryLogs` 和 MCP 日志工具；若模型调了本地这个，会拿到 `queryLogs` 里那句 "如果配置文件没有用 mock……不会走进来" 的错误字符串，污染推理、可能导致报告不准。
- **建议修法**：给 `QueryLogsTools` 加 `@ConditionalOnProperty(name = "cls.mock-enabled", havingValue = "true")`，让它只在 mock 模式建 bean；这样 `required=false` + `buildMethodToolsArray()` 的原设计才成立。
- **面试价值**：能主动发现并讲清"注解条件化装配 + `required=false` 的配合"是很好的加分点。

> （后续发现的 bug 依次往下加：#2、#3……）

---

## 6. 完整运行环境清单（活清单）

### 6.1 基础软件
| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | **17**（`pom.xml` 锁定 source/target 17） | 高于/低于都可能编译或运行异常 |
| Maven | 3.6+ | 构建工具；也可用 IDEA 内置 |
| Docker + Docker Compose | 新版本 | 起 Milvus 全家桶（`vector-database.yml`） |
| 操作系统 | 任意 | Makefile 用的是 bash/curl，Windows 下建议用 Git Bash 或 WSL 跑 `make` |

### 6.2 必需的环境变量
```bash
export DASHSCOPE_API_KEY=<你的阿里云 DashScope Key>
```
- 同一个 Key 被两处读取：`spring.ai.dashscope.api-key`（聊天/Agent）与 `dashscope.api.key`（embedding & RagService）。
- 未设置时 `VectorEmbeddingService` 会在启动时**直接抛异常**（它校验了占位符 `your-api-key-here`）。

### 6.3 依赖的外部服务（`vector-database.yml` 起的容器）
| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| Milvus standalone | milvus-standalone | 19530（gRPC）/ 9091（health） | 向量库主服务 |
| etcd | milvus-etcd | — | Milvus 元数据 |
| MinIO | milvus-minio | 9000 / 9001(控制台 admin/minioadmin) | Milvus 对象存储 |
| Attu | milvus-attu | 8000→3000 | Milvus 可视化 UI |
| （可选）Prometheus | 需自建 | 9090 | 真实告警源；不想装就开 mock |
| （可选）腾讯云 CLS MCP | 云端 SSE | — | 真实日志源；不想接就开 mock |

> 注意：compose 里 Milvus 镜像是 `milvusdb/milvus:v2.5.10`，而 `pom.xml` 的 SDK 是 `milvus-sdk-java:2.6.10`。大版本一致（2.x），通常兼容，但值得心里有数（面试可能被问"服务端和客户端版本对不对得上"）。

### 6.4 推荐的"纯本地自洽"跑法（不依赖 Prometheus / 腾讯云）
想在一台机器上零外部依赖跑通 demo，建议改 `application.yml`：
1. `prometheus.mock-enabled: true` —— 用内置的假告警。
2. `cls.mock-enabled: true` —— 用内置的假日志（且与假告警交叉呼应，能跑出完整报告）。
3. **注释掉 `spring.ai.mcp.client` 整段**（或 `enabled: false`）——否则启动会尝试连 `mcp-api.tencent-cloud.com` 那个占位 `/sse/92XXXXXXXXb4` 端点而失败。
> ⚠️ 但见 Bug #1：即便开了 mock，真实模式的排除逻辑本身有缺陷；纯 mock 跑 demo 不受影响。

### 6.5 启动顺序（关键！）
```bash
# 1) 先起向量库（必须先于应用）
docker compose -f vector-database.yml up -d      # 或 make up

# 2) 设 Key
export DASHSCOPE_API_KEY=xxx

# 3) 编译 + 起服务
mvn clean install
mvn spring-boot:run                               # 或 make start（后台）

# 4) 灌入运维知识库（把 aiops-docs/*.md 向量化）
make upload

# 一键版：make init 会把 1→4 串起来
```
访问：应用 `http://localhost:9900`，Attu `http://localhost:8000`。

---

## 7. 下一步行动（Claude 的待办 · 每次更新）

- [ ] 按第 3 节表格，逐块给我做**深度讲解**（建议从 #1 架构、#3 AIOps 编排入手，这两块最出彩也最容易被追问）。
- [ ] 每讲完一块，更新第 3 节状态 + 第 4 节问答库。
- [ ] 继续扫 Bug，新增进第 5 节（先报告）。
- [ ] 校核第 6 节环境清单，必要时实际跑一遍验证。

> **每次开工提醒自己**：目标不是把活干完，是把**我**教会到能上考场。
