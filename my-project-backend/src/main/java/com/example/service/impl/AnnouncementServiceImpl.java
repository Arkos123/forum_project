package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.dto.Announcement;
import com.example.entity.vo.request.AnnouncementCreateVO;
import com.example.entity.vo.request.AnnouncementUpdateVO;
import com.example.entity.vo.response.AnnouncementAdminVO;
import com.example.entity.vo.response.AnnouncementDetailVO;
import com.example.entity.vo.response.AnnouncementPreviewVO;
import com.example.mapper.AccountMapper;
import com.example.mapper.AnnouncementMapper;
import com.example.service.AnnouncementService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Resource
    AccountMapper accountMapper;

    @Override
    public List<AnnouncementPreviewVO> latest(int limit) {
        int size = Math.max(1, Math.min(limit, 10));
        Page<Announcement> page = Page.of(1, size);
        baseMapper.selectPage(page, publishedQuery());
        return page.getRecords().stream().map(this::preview).toList();
    }

    @Override
    public Page<AnnouncementPreviewVO> listPublished(int page, int size) {
        Page<Announcement> result = baseMapper.selectPage(Page.of(page, size), publishedQuery());
        return convertPage(result, this::preview);
    }

    @Override
    public AnnouncementDetailVO detail(int id) {
        Announcement announcement = baseMapper.selectOne(Wrappers.<Announcement>query()
                .eq("id", id)
                .eq("published", 1));
        return announcement == null ? null : announcement.asViewObject(AnnouncementDetailVO.class);
    }

    @Override
    public Page<AnnouncementAdminVO> listAll(int page, int size, String keyword, Boolean published) {
        QueryWrapper<Announcement> query = Wrappers.<Announcement>query()
                .eq(published != null, "published", published)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like("title", keyword)
                        .or()
                        .like("summary", keyword))
                .orderByDesc("top")
                .orderByDesc("create_time")
                .orderByDesc("id");
        Page<Announcement> result = baseMapper.selectPage(Page.of(page, size), query);
        return convertPage(result, this::admin);
    }

    @Override
    public int create(int uid, AnnouncementCreateVO vo) {
        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(vo, announcement);
        announcement.setContent(vo.getContent().toJSONString());
        announcement.setUid(uid);
        announcement.setPublished(false);
        announcement.setTop(false);
        Date now = new Date();
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        baseMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public String update(AnnouncementUpdateVO vo) {
        Announcement announcement = baseMapper.selectById(vo.getId());
        if(announcement == null)
            return "公告不存在";
        announcement.setTitle(vo.getTitle());
        announcement.setSummary(vo.getSummary());
        announcement.setContent(vo.getContent().toJSONString());
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public String publish(int id, boolean published) {
        Announcement announcement = baseMapper.selectById(id);
        if(announcement == null)
            return "公告不存在";
        if(!Boolean.TRUE.equals(announcement.getPublished()) && published)
            announcement.setPublishTime(new Date());
        announcement.setPublished(published);
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public String top(int id, boolean top) {
        Announcement announcement = baseMapper.selectById(id);
        if(announcement == null)
            return "公告不存在";
        announcement.setTop(top);
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public void delete(int id) {
        baseMapper.deleteById(id);
    }

    private QueryWrapper<Announcement> publishedQuery() {
        return Wrappers.<Announcement>query()
                .eq("published", 1)
                .orderByDesc("top")
                .orderByDesc("publish_time")
                .orderByDesc("id");
    }

    private AnnouncementPreviewVO preview(Announcement announcement) {
        return announcement.asViewObject(AnnouncementPreviewVO.class);
    }

    private AnnouncementAdminVO admin(Announcement announcement) {
        AnnouncementAdminVO vo = announcement.asViewObject(AnnouncementAdminVO.class);
        Account account = accountMapper.selectById(announcement.getUid());
        if(account != null)
            vo.setUsername(account.getUsername());
        return vo;
    }

    private <T> Page<T> convertPage(Page<Announcement> source, Function<Announcement, T> converter) {
        Page<T> target = Page.of(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream().map(converter).toList());
        return target;
    }
}
