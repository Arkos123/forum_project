package com.example.service;

import com.example.entity.es.TopicDocument;
import com.example.repository.TopicRepository;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 注册给 AI 的可调用工具。
 * Spring AI 会自动发现 @Tool 注解的方法，让 AI 自主决策调用哪些工具。
 */
@Component
public class ForumTools {

    @Resource
    private TopicRepository topicRepository;

    @Tool(name = "search_forum_posts", description = "根据关键词搜索论坛帖子。当用户提出具体问题、想知道论坛里有没有相关内容时调用此工具。")
    public String searchForumPosts(
            @ToolParam(description = "搜索关键词，可以是单个词或短语") String keyword) {

        List<SearchHit<TopicDocument>> hits = topicRepository.findByTitleOrIntro(keyword);

        if (hits == null || hits.isEmpty()) {
            return "未找到与「" + keyword + "」相关的帖子。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索「").append(keyword).append("」找到 ").append(hits.size()).append(" 条相关帖子：\n\n");
        int count = 0;
        for (SearchHit<TopicDocument> hit : hits) {
            if (count >= 5) break;
            TopicDocument doc = hit.getContent();
            sb.append(count + 1).append(". 【").append(doc.getTitle()).append("】\n");
            sb.append("   ").append(doc.getIntro()).append("\n\n");
            count++;
        }
        return sb.toString();
    }

    @Tool(name = "get_recent_posts", description = "获取论坛最新的帖子列表。当用户想了解论坛最近在讨论什么、想浏览或总结论坛整体内容时调用此工具。")
    public String getRecentPosts(
            @ToolParam(description = "获取多少条帖子，范围1-10，默认5") int count) {

        if (count < 1) count = 5;
        if (count > 10) count = 10;

        List<TopicDocument> docs = topicRepository.findByOrderByTimeDesc(PageRequest.of(0, count));

        if (docs == null || docs.isEmpty()) {
            return "论坛暂无帖子。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("论坛最新 ").append(docs.size()).append(" 条帖子：\n\n");
        for (int i = 0; i < docs.size(); i++) {
            TopicDocument doc = docs.get(i);
            sb.append(i + 1).append(". 【").append(doc.getTitle()).append("】\n");
            sb.append("   ").append(doc.getIntro()).append("\n\n");
        }
        return sb.toString();
    }
}