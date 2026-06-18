package com.example.announcement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.announcement.entity.vo.response.AnnouncementDetailVO;
import com.example.announcement.entity.vo.response.AnnouncementPreviewVO;
import com.example.announcement.service.AnnouncementService;
import com.example.common.entity.PageRestBean;
import com.example.common.entity.RestBean;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Resource
    AnnouncementService service;

    @GetMapping("/latest")
    public RestBean<List<AnnouncementPreviewVO>> latest(@RequestParam(defaultValue = "3") @Min(1) @Max(10) int limit) {
        return RestBean.success(service.latest(limit));
    }

    @GetMapping("/list")
    public PageRestBean<AnnouncementPreviewVO> list(@RequestParam @Min(0) int page,
                                                    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Page<AnnouncementPreviewVO> result = service.listPublished(page + 1, size);
        return PageRestBean.success(result.getRecords(), result.getTotal(), result.getCurrent());
    }

    @GetMapping("/detail")
    public RestBean<AnnouncementDetailVO> detail(@RequestParam @Min(1) int id) {
        AnnouncementDetailVO vo = service.detail(id);
        if (vo == null)
            return RestBean.failure(404, "公告不存在或已下线");
        return RestBean.success(vo);
    }
}
