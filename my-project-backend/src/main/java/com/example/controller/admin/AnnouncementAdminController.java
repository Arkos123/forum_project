package com.example.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.entity.PageRestBean;
import com.example.common.entity.RestBean;
import com.example.entity.vo.request.AnnouncementCreateVO;
import com.example.entity.vo.request.AnnouncementPublishVO;
import com.example.entity.vo.request.AnnouncementTopVO;
import com.example.entity.vo.request.AnnouncementUpdateVO;
import com.example.entity.vo.response.AnnouncementAdminVO;
import com.example.service.AnnouncementService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/announcement")
public class AnnouncementAdminController {

    @Resource
    AnnouncementService service;

    @GetMapping("/list")
    public PageRestBean<AnnouncementAdminVO> list(@RequestParam @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean published) {
        Page<AnnouncementAdminVO> result = service.listAll(page, size, keyword, published);
        return PageRestBean.success(result.getRecords(), result.getTotal(), result.getCurrent());
    }

    @PostMapping("/create")
    public RestBean<Integer> create(@RequestBody @Valid AnnouncementCreateVO vo,
                                    @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(service.create(uid, vo));
    }

    @PostMapping("/update")
    public RestBean<Void> update(@RequestBody @Valid AnnouncementUpdateVO vo) {
        return messageHandle(service.update(vo));
    }

    @PostMapping("/publish")
    public RestBean<Void> publish(@RequestBody @Valid AnnouncementPublishVO vo) {
        return messageHandle(service.publish(vo.getId(), vo.getPublished()));
    }

    @PostMapping("/top")
    public RestBean<Void> top(@RequestBody @Valid AnnouncementTopVO vo) {
        return messageHandle(service.top(vo.getId(), vo.getTop()));
    }

    @GetMapping("/delete")
    public RestBean<Void> delete(@RequestParam @Min(1) int id) {
        service.delete(id);
        return RestBean.success();
    }

    private RestBean<Void> messageHandle(String message) {
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
