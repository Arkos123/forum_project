package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.service.AiService;
import com.example.service.ForumTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    @Resource
    ChatModel chatModel;

    @Resource
    ForumTools forumTools;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是校园论坛的AI助手。你可以使用工具来查询论坛数据。
                        用户提问时，如果涉及论坛内容，主动调用工具查询。
                        基于工具返回的真实帖子内容来回答用户问题。
                        禁止说"无法访问论坛""没有权限"之类的话。
                        用中文回答。
                        """)
                .defaultTools(forumTools)
                .build();
    }

    @Override
    public SseEmitter chatWithAi(JSONArray context) {
        SseEmitter emitter = new SseEmitter(30000L);

        // 将上下文转换为 Message 列表
        List<Message> messages = new ArrayList<>();
        for (Object item : context) {
            JSONObject obj = JSONObject.from(item);
            Message msg = switch (obj.getString("type")) {
                case "user" -> new UserMessage(obj.getString("text"));
                case "assistant" -> new AssistantMessage(obj.getString("text"));
                default -> throw new RuntimeException("Unknown message type: " + obj.getString("type"));
            };
            messages.add(msg);
        }

        // ChatClient 自动处理工具调用循环：AI 请求工具 → 执行 → 返回结果 → AI 回复
        Flux<String> flux = this.chatClient.prompt()
                .messages(messages)
                .stream()
                .content();

        flux.subscribe(text -> {
            try {
                emitter.send(text);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);

        return emitter;
    }
}
