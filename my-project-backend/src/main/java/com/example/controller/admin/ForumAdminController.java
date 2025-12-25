package com.example.controller.admin;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.PageRestBean;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.service.TopicService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/forum")
public class ForumAdminController {

    @Resource
    private TopicService service;

    @GetMapping("/list")
    public PageRestBean<TopicPreviewVO> list(@RequestParam int page,
                                             @RequestParam int size) {
        JSONObject result = service.listAllTopicByPage(page, size);
        return PageRestBean.success(
                result.getJSONArray("list").toList(TopicPreviewVO.class),
                result.getIntValue("total"),
                page
        );
    }
}
