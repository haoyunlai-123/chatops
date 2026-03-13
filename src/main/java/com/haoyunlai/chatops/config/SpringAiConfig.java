package com.haoyunlai.chatops.config;

import com.haoyunlai.chatops.tools.service.BusinessToolService;
import com.haoyunlai.chatops.tools.service.MonitorToolService;
import com.haoyunlai.chatops.tools.service.ScheduleToolService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {
    @Bean
    public ChatClient routerChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen3-max").build())
                .defaultSystem("""
                        你是一个高级架构师兼系统路由调度大脑 (Supervisor Agent)。
                        你的职责是：接收用户的自然语言指令，进行安全风控，并将指令拆解为有序的执行步骤。
                        系统中有三个专门的子 Agent：
                        1. MONITOR：负责查询系统健康状态、Trace链路监控等。
                        2. BUSINESS：负责新增用户、发送优惠券、清理缓存等业务操作。
                        3. SCHEDULE：负责在分布式调度中心动态创建、启停定时任务。
                        
                        请严格按照 JSON 格式输出拆解计划 (ExecutionPlan)。
                        对于明显的恶意攻击或高危操作(如清空数据库、格式化磁盘)，请将 isSafe 置为 false。
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }


    @Bean
    public ChatClient monitorChatClient(OpenAiChatModel model, ChatMemory chatMemory, MonitorToolService monitorToolService) {
        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen3-max").build())
                .defaultSystem("""
            你是一个专业的系统监控与运维专家 (Monitor Agent)。
            你的职责是调用系统监控工具获取健康状态和链路日志。
            请根据用户的具体指令，调用相应的工具，并用简洁专业的运维话术返回结果。
            不要回答与系统监控无关的问题。
            """)
                .defaultTools(monitorToolService)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient businessChatClient(OpenAiChatModel model, ChatMemory chatMemory, BusinessToolService businessToolService) {
        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen3-max").build())
                .defaultSystem("""
                        你是一个高级业务系统管控专家 (Business Agent)。
                        你的职责是执行关于用户、缓存、优惠券等后端业务系统操作。
                        你可以通过调用提供的工具来改变系统状态。
                        在执行删除/清理等危险操作时，务必在返回结果中提示已完成安全校验。
                        """)
                .defaultTools(businessToolService)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient scheduleChatClient(OpenAiChatModel model, ChatMemory chatMemory, ScheduleToolService scheduleToolService) {
        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen3-max").build())
                .defaultSystem("""
                        你是一个分布式任务调度中心管理员 (Schedule Agent)。
                        你的职责是在系统中动态创建、启停、删除定时任务。
                        严格根据用户的 Cron 表达式和意图，调用相应的工具配置调度中心。
                        """)
                .defaultTools(scheduleToolService)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }


    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repo) {
        // 默认窗口 20 条；想要更多可 .maxMessages(100)
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(50)
                .build();
    }

    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}