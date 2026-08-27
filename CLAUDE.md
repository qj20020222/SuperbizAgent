# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> ⚠️ **先读 `PROJECT_COMPASS.md`（项目北极星）再动手。** 项目主人是一名求职者，本项目是简历核心项目。第一使命是**把他讲懂、能扛住面试追问**；其次是**发现 Bug（先报告后修）**；再次是**给全可运行环境**。每次工作前先读 COMPASS 的初衷与协作约定，工作后更新它的进度表 / 问答库 / Bug 清单。本文件（CLAUDE.md）只是技术速查。

## Overview

SuperBizAgent is a Spring Boot 3.2 / Java 17 application built on the **Spring AI Alibaba** agent framework (`spring-ai-alibaba-agent-framework` + graph). It exposes two AI subsystems over one HTTP server (port `9900`):

1. **Intelligent chat (RAG-backed)** — a single-turn/streaming assistant driven by a `ReactAgent` that auto-invokes tools (internal-doc retrieval over Milvus, Prometheus alerts, time, and — in real mode — Tencent CLS logs via MCP).
2. **AIOps** — an automated alert-triage pipeline using a multi-agent **Supervisor → Planner/Replanner + Executor** graph that produces a fixed-template Markdown incident report.

## Commands

Requires `DASHSCOPE_API_KEY` in the environment (Alibaba DashScope key, used for both chat/embedding). Milvus must be running before the app starts.

```bash
export DASHSCOPE_API_KEY=your-api-key

# Build
mvn clean install

# Run the app (foreground)
mvn spring-boot:run          # main class: org.example.Main

# Full one-shot bootstrap: start Milvus (Docker) → start app in background → upload aiops-docs/ to the vector store
make init

# Milvus lifecycle (docker compose, file is vector-database.yml)
make up / make down / make status

# App lifecycle helpers (backgrounds mvn spring-boot:run into server.log / server.pid)
make start / make stop / make restart / make check

# Vectorize the knowledge base (POSTs every aiops-docs/*.md to /api/upload)
make upload
make test-upload             # upload a single file, useful smoke test
```

There is **no test suite** in this repo (`src/test` does not exist), so "run a single test" does not apply. Verify changes by exercising the HTTP endpoints (see below) or `make check` against a running server.

Service URLs when running: app `http://localhost:9900`, Milvus `localhost:19530`, Attu UI `http://localhost:8000`, MinIO console `http://localhost:9001` (admin/minioadmin).

## Key HTTP endpoints (`ChatController`, `FileUploadController`, `MilvusCheckController`)

- `POST /api/chat` and `POST /api/chat_stream` (SSE) — chat. Body: `{"Id": "<sessionId>", "Question": "..."}` (Jackson aliases accept `id`/`question` too). SSE messages are JSON `{type: content|error|done, data}`.
- `POST /api/ai_ops` (SSE) — runs the AIOps multi-agent pipeline; takes no meaningful input.
- `POST /api/chat/clear`, `GET /api/chat/session/{sessionId}` — session management.
- `POST /api/upload` (multipart `file`) — saves to `./uploads` then auto-vectorizes into Milvus. Only `txt,md` allowed; filename-based dedup (re-upload overwrites).
- `GET /milvus/health` — Milvus health check (used by the Makefile readiness gate).

## Architecture notes (the non-obvious parts)

**Mock mode fundamentally changes tool wiring.** Two independent flags in `application.yml`:
- `prometheus.mock-enabled` — when true, `QueryMetricsTools` returns canned alerts (`HighCPUUsage`, `HighMemoryUsage`, `SlowResponse`, …) instead of hitting `prometheus.base-url`.
- `cls.mock-enabled` — controls **whether the `QueryLogsTools` bean even exists**. It's injected as `@Autowired(required = false)` in `ChatService` and `AiOpsService`; `buildMethodToolsArray()` includes it only when non-null. When `cls.mock-enabled=false`, log querying is instead provided by the **Tencent CLS MCP server** (configured under `spring.ai.mcp.client` and injected via `ToolCallbackProvider`). The mock alert data and mock log data are deliberately cross-referenced (each alert name maps to related log topics/queries) so the AIOps demo produces a coherent report end-to-end.

**Two tool families feed every agent.** `methodTools(...)` = local `@Tool`-annotated Spring beans in `org.example.agent.tool` (`DateTimeTools`, `InternalDocsTools`, `QueryMetricsTools`, optional `QueryLogsTools`). `tools(toolCallbacks)` = MCP-provided tools from `ToolCallbackProvider.getToolCallbacks()`. Both `ChatService` and `AiOpsService` duplicate a `buildMethodToolsArray()` helper — keep them in sync when changing the tool set.

**AIOps agent graph** (`AiOpsService`): a `SupervisorAgent` orchestrates two `ReactAgent`s. Planner writes to state key `planner_plan`, Executor to `executor_feedback`; the supervisor loops PLAN→EXECUTE until `decision=FINISH`. The **final report is extracted from the `planner_plan` state value** (an `AssistantMessage`), not from the supervisor's return text — see `extractFinalReport`. Agent behavior is governed almost entirely by the large system prompts hardcoded in `AiOpsService` (Planner/Executor/Supervisor), including the mandatory report template and the anti-hallucination "stop after 3 failed tool calls" rule. Prompt changes there are the main lever for AIOps output.

**Chat sessions** are in-memory only (`ConcurrentHashMap` in `ChatController`, `SessionInfo` inner class), lost on restart. History is a sliding window of `MAX_WINDOW_SIZE = 6` pairs and is injected into the system prompt as plain text by `ChatService.buildSystemPrompt` (no framework-level memory).

**RAG / Milvus store.** Collection `biz`, vector dim **1024**, model `text-embedding-v4` (DashScope). Upload flow: `FileUploadController` → `VectorIndexService.indexSingleFile` → `DocumentChunkService` (chunk size `document.chunk.max-size=800`, overlap `100`) → `VectorEmbeddingService` (DashScope embeddings) → Milvus. Retrieval at chat time happens through the `InternalDocsTools` tool the agent calls, using `VectorSearchService` with `rag.top-k=3`. Note: `MilvusConstants.VECTOR_DIM` comments say "豆包/1024" but embeddings are actually generated via DashScope `text-embedding-v4` — the dimension, not the vendor comment, is what matters.

**Two DashScope integration paths coexist.** Chat/agents use the Spring AI Alibaba starter (`DashScopeChatModel`, model `DashScopeChatModel.DEFAULT_MODEL_NAME`, configured in `ChatService.createChatModel`). Embedding + the standalone `RagService` use the lower-level `dashscope-sdk-java` with the global `Constants.apiKey`. Both read the same key but from different config properties: `spring.ai.dashscope.api-key` vs. `dashscope.api.key`.

**`RagService` is currently not wired to any controller** — the live chat endpoints go through `ChatService` + `ReactAgent`, not `RagService.queryStream`. Treat `RagService` as a standalone/legacy RAG path; don't assume editing it changes `/api/chat` behavior.

## Config surface (`src/main/resources/application.yml`)

Everything toggleable lives here: `server.port`, `milvus.*`, `spring.ai.dashscope.*` (retry/timeout), `spring.ai.mcp.client.*` (Tencent CLS SSE endpoint — comment out to run fully mock), `dashscope.embedding.model`, `document.chunk.*`, `rag.top-k`/`rag.model`, `prometheus.*`, `cls.mock-enabled`. Static test UI is served from `src/main/resources/static/` (`index.html`, `app.js`).
