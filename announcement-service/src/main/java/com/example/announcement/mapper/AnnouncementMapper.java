package com.example.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.announcement.entity.dto.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
