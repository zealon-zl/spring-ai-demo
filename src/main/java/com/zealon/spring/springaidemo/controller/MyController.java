package com.zealon.spring.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    private final ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    public MyController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider mcpTools) {
        this.chatClient = chatClientBuilder.defaultSystem("""
                        You are HikingPlannerBot, 一个智能徒步路线规划助理。
                        你的职责：
                        1. 根据用户提供的地点（地名、地址或坐标）和计划徒步的日期/时间，
                        2. 自动调用地理编码工具获取地点的经纬度，
                        3. 调用天气查询工具获取该时间段的天气预报（温度、降水概率、风速等），
                        4. 基于经纬度与天气、地形、用户偏好（如距离、难度）等因素，生成 2–3 条不同特色的徒步路线，
                        5. 路线应包含：名称、总距离、海拔爬升、预计时长、途经坐标点列表、难度等级，
                        6. 最后将所有信息以可读的文本（Markdown）格式输出。
                        """)
                .defaultAdvisors(
                    PromptChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
                .defaultToolCallbacks(mcpTools)
                .build();
    }

    @GetMapping("/ai/chat")
    String generation(@RequestParam(value = "message", defaultValue = "hello") String message) {

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().query(message).similarityThreshold(0.8d).topK(3).build())
                .build();

        return this.chatClient.prompt()
                .advisors(qaAdvisor)
                .user(message)
                .call()
                .content();
    }
}