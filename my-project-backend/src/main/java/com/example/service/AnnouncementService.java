package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Announcement;
import com.example.entity.vo.request.AnnouncementCreateVO;
import com.example.entity.vo.request.AnnouncementUpdateVO;
import com.example.entity.vo.response.AnnouncementAdminVO;
import com.example.entity.vo.response.AnnouncementDetailVO;
import com.example.entity.vo.response.AnnouncementPreviewVO;

import java.util.List;

public interface AnnouncementService extends IService<Announcement> {
    List<AnnouncementPreviewVO> latest(int limit);
    Page<AnnouncementPreviewVO> listPublished(int page, int size);
    AnnouncementDetailVO detail(int id);
    Page<AnnouncementAdminVO> listAll(int page, int size, String keyword, Boolean published);
    int create(int uid, AnnouncementCreateVO vo);
    String update(AnnouncementUpdateVO vo);
    String publish(int id, boolean published);
    String top(int id, boolean top);
    void delete(int id);
}
