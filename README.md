
***

#  AIOps 管控模块

本项目是 [分布式任务调度系统与自研网关/RPC] 的智能化管控子模块。
基于 Spring AI 框架，通过引入大语言模型（LLM）的 Tool Calling 与结构化输出能力，将底层系统复杂的 API 调用（网关健康检查、RPC 泛化调用、调度任务下发等）转化为自然语言驱动的自动化工作流。

系统侧重于解决 AI 接入后台时面临的**上下文孤岛、调用不稳定**以及**高危写操作不可控**等工程落地痛点。

##  核心机制与特性

*   **多智能体路由架构**
    *   采用路由总控模式。`RouterAgent` 负责对用户的自然语言指令进行安全风控、意图拆解，并生成基于 JSON 的有向无环图 (DAG) 执行计划 (`ExecutionPlan`)。
    *   任务被拆解后，按序路由给底层的专业执行器 (`MonitorAgent`, `BusinessAgent`, `ScheduleAgent`)，避免单一 Agent 上下文超载与幻觉。
*   **基于黑板模式的全局上下文共享**
    *   在多步骤执行链路中，维护一个全局 `StringBuilder` 作为上下文。上游 Agent 执行返回的业务 ID（如新增用户产生的 UserID），会自动作为动态 Prompt 拼接给下游 Agent，实现多步骤间的参数无缝传递。
*   **异常捕获与自我反思修正**
    *   由于 LLM 提取参数存在不稳定性，系统在底层工具层增加了强校验。当调用抛出 `IllegalArgumentException` 等业务异常时，拦截器会抓取 Error Message，结合 ChatMemory 历史记忆生成反思 Prompt，引导 LLM 自动修正参数并重试，提升工作流容错率。
*   **基于状态机的挂起与人工审批流**
    *   严格风控机制：当 LLM 规划的执行步骤中包含高危操作（如清理缓存、删除任务）时，`requireApproval` 标志位触发。
    *   系统会中断并**挂起 (Suspend)** 当前执行链路，将上下文快照存入缓存，并向下发发审批 Token。待管理员调用审批接口后，系统基于 Token **唤醒 (Resume)** 状态机，继续执行后续步骤，保障零信任架构下的系统安全。
*   **RAG 向量检索与 AIOps 故障诊断**
    *   在应用启动阶段，通过 `TokenTextSplitter` 读取系统架构文档与错误码 Runbook (`/docs/*.txt`)，存入内存级 `VectorStore`。
    *   配置专属的 `TroubleshootAgent` 结合 `QuestionAnswerAdvisor`，当系统发生异常（如网关 503、TraceId 丢失）时，提供基于内部知识库的专业排查建议。
*   **SSE 响应式流式输出**
    *   针对多智能体长链路执行导致的 HTTP 阻塞问题，控制器采用 `SseEmitter` 异步非阻塞推送。Agent 的思考过程、执行日志、异常重试信息均以实时流的方式推送到前端。

##  技术栈

*   **基础框架**: Spring Boot 3.x, Java 17
*   **AI 框架**: Spring AI (1.0.0-M1+), 兼容 OpenAI API 规范的模型接入 (如 Qwen / DeepSeek)
*   **核心组件**: Tool Calling (Function Calling), BeanOutputConverter, ChatMemory, SimpleVectorStore, SseEmitter

##  核心目录结构

```text
\chatops
├── README.md
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── haoyunlai/
        │           └── chatops/
        │               ├── ChatopsApplication.java
        │               ├── agent/
        │               │   ├── BusinessWorkerAgent.java
        │               │   ├── MonitorWorkerAgent.java
        │               │   ├── RouterAgent.java
        │               │   ├── ScheduleWorkerAgent.java
        │               │   └── TroubleshootWorkerAgent.java
        │               ├── config/
        │               │   ├── KnowledgeBaseInitializer.java
        │               │   ├── MvcConfiguration.java
        │               │   ├── PromptTemplateConfig.java
        │               │   └── SpringAiConfig.java
        │               ├── controller/
        │               │   └── ChatAgentController.java
        │               ├── model/
        │               │   ├── ChatRequest.java
        │               │   ├── ChatResponse.java
        │               │   └── plan/
        │               │       ├── ExecutionPlan.java
        │               │       └── Step.java
        │               └── tools/
        │                   ├── config/
        │                   │   └── AgentToolConfig.java
        │                   └── service/
        │                       ├── BusinessToolService.java
        │                       ├── MonitorToolService.java
        │                       ├── RouterToolService.java
        │                       └── ScheduleToolService.java
        └── resources/
            ├── application.yml
            └── docs/
                ├── error-codes.txt
                └── system-architecture.txt

```

##  快速启动

1.  **环境准备**: 确保已安装 JDK 17+ 及 Maven。
2.  **配置 API Key**: 打开 `src/main/resources/application.yml` (或 `.properties`)，填入兼容 OpenAI 格式的大模型服务商配置：
    ```yaml
    spring:
      ai:
        openai:
          base-url: https://dashscope.aliyuncs.com/compatible-mode/v1 # 以阿里百炼为例
          api-key: sk-your-api-key-here
          chat:
            options:
              model: qwen-plus # 或 deepseek-chat
              temperature: 0.2 # 建议调低温度以保证工具调用的严谨性
    ```
3.  **启动服务**: 运行 `ChatopsApplication.java`。启动时控制台会打印 RAG 知识库加载日志。

## API 使用示例 (基于 Postman 或浏览器)

### 1. 提交管控指令 (实时流输出)
*   **接口**: `POST http://localhost:8080/api/chatops/execute`
*   **Headers**: `Content-Type: application/json`, `Accept: text/event-stream`
*   **Body**:
    ```json
    {
      "userId": "admin_123",
      "message": "查一下网关健康状态。没问题的话，新增一个名为'测试'的用户（需发券），最后建一个Cron为'0/5 * * * * ?'的调度任务。"
    }
    ```
*   **效果**: 返回流式数据，可实时看到 Agent 的意图拆解、参数纠错和分步执行日志。

### 2. 触发高危操作与人工审批 (HITL)
*   **场景**: 发送 `清理包含 login 的缓存` 指令。
*   **现象**: 执行流挂起，最后一条推送为：
    `data: ⏸️ 警告：该步骤属于高危写操作！流程已挂起。审批凭证: HITL-A1B2C3D4`
*   **唤醒接口**: `GET http://localhost:8080/api/chatops/approve?token=HITL-A1B2C3D4`
    *(支持在浏览器直接打开，观察后续步骤被唤醒并流式输出的执行过程)*。