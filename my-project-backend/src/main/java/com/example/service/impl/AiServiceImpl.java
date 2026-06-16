package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.service.AiConversationService;
import com.example.service.AiService;
import com.example.service.ForumTools;
import com.example.service.WebSearchTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    @Resource
    ChatModel chatModel;

    @Resource
    ForumTools forumTools;

    @Resource
    WebSearchTools webSearchTools;

    @Resource
    AiConversationService conversationService;

    ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是校园论坛的AI助手，名字叫「校园AI助手」。你友善、专业，能够帮助学生解答问题。" +
                        "你可以在论坛中搜索帖子、获取最新的帖子信息。" +
                        "当需要互联网上的最新信息时，你可以进行网络搜索。" +
                        "请用中文回复。")
                .build();
    }

    @Override
    public SseEmitter chatWithAi(JSONArray context) {
        SseEmitter emitter = new SseEmitter(30000L);
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

    @Override
    public SseEmitter chatWithAi(int conversationId, int userId, String text,
                                 List<String> imageUrls, boolean enableWebSearch) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            // 1. 从数据库加载历史消息
            List<JSONObject> history = conversationService.loadMessages(userId, conversationId);
            List<Message> messages = new ArrayList<>();

            // 2. 将历史消息转为 Spring AI Message
            for (JSONObject msg : history) {
                String role = msg.getString("type");
                String content = msg.getString("text");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }

            // 3. 构建当前用户消息（含可能的图片）
            if (imageUrls != null && !imageUrls.isEmpty()) {
                var userMsgBuilder = UserMessage.builder().text(text);
                for (String url : imageUrls) {
                    MimeType mime = detectImageMimeType(url);
                    userMsgBuilder.media(Media.builder()
                            .mimeType(mime)
                            .data(url)
                            .build());
                }
                messages.add(userMsgBuilder.build());
            } else {
                messages.add(new UserMessage(text));
            }

            // 4. 保存用户消息到数据库
            JSONObject userContent = new JSONObject();
            userContent.put("text", text);
            if (imageUrls != null && !imageUrls.isEmpty()) {
                userContent.put("imageUrls", imageUrls);
            }
            conversationService.saveMessage(userId, conversationId, "user",
                    userContent.toJSONString(),
                    (imageUrls != null && !imageUrls.isEmpty()) ? "image" : "text");

            // 5. 构建 ChatClient 请求
            var promptSpec = chatClient.prompt().messages(messages);

            // 6. 注册工具
            List<ToolCallback> toolCallbacks = new ArrayList<>();
            Collections.addAll(toolCallbacks, ToolCallbacks.from(forumTools));
            if (enableWebSearch) {
                Collections.addAll(toolCallbacks, ToolCallbacks.from(webSearchTools));
            }
            promptSpec.toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]));

            // 7. 流式调用，发送 SSE
            Flux<String> flux = promptSpec.stream().content();
            StringBuilder fullReply = new StringBuilder();

            flux.subscribe(
                    chunk -> {
                        fullReply.append(chunk);
                        try {
                            JSONObject json = new JSONObject();
                            json.put("type", "text");
                            json.put("content", chunk);
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(json.toJSONString()));
                        } catch (IOException e) {
                            // ignore
                        }
                    },
                    error -> {
                        try {
                            JSONObject errJson = new JSONObject();
                            errJson.put("type", "error");
                            errJson.put("content", error.getMessage() != null ? error.getMessage() : "unknown");
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(errJson.toJSONString()));
                        } catch (IOException e) {
                            // ignore
                        }
                        emitter.completeWithError(error);
                    },
                    () -> {
                        // 8. 保存完整 AI 回复到数据库
                        if (fullReply.length() > 0) {
                            conversationService.saveMessage(userId, conversationId, "assistant",
                                    fullReply.toString(), "text");
                        }
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(""));
                        } catch (IOException e) {
                            // ignore
                        }
                        emitter.complete();
                    }
            );

        } catch (IllegalArgumentException e) {
            try {
                JSONObject errJson = new JSONObject();
                errJson.put("type", "error");
                errJson.put("content", e.getMessage());
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errJson.toJSONString()));
            } catch (IOException ex) {
                // ignore
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 根据图片URL后缀检测MIME类型
     */
    private MimeType detectImageMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return MimeTypeUtils.IMAGE_JPEG;
        else if (lower.endsWith(".gif"))
            return MimeTypeUtils.IMAGE_GIF;
        else if (lower.endsWith(".webp"))
            return MimeTypeUtils.parseMimeType("image/webp");
        else if (lower.endsWith(".bmp"))
            return MimeTypeUtils.parseMimeType("image/bmp");
        else // 默认 PNG（含 .png 和无后缀情况）
            return MimeTypeUtils.IMAGE_PNG;
    }
}
