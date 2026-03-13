package com.haoyunlai.chatops.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private final VectorStore vectorStore;

    // 自动扫描 classpath:docs/ 下的所有 txt 文件
    @Value("classpath:docs/*.txt")
    private Resource[] docResources;

    public KnowledgeBaseInitializer(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("📚 [RAG 知识库] 正在初始化本地向量数据库...");

        if (docResources == null || docResources.length == 0) {
            log.warn("⚠️ 未找到任何本地知识库文档。");
            return;
        }

        // 用于将长文本切分为小块 (Chunking) 以提升检索精度
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        for (Resource resource : docResources) {
            log.info("📖 正在加载文档: {}", resource.getFilename());
            TextReader textReader = new TextReader(resource);
            // 增量式将读取的文本进行切分并写入 VectorStore
            vectorStore.accept(textSplitter.apply(textReader.get()));
        }

        log.info("✅ [RAG 知识库] 向量数据库初始化完毕，系统已具备 AIOps 诊断能力！");
    }
}