package com.example.announcement.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementAdminVO {
    Integer id;
    Integer uid;
    String username;
    String title;
    String summary;
    String content;
    Boolean published;
    Boolean top;
    Date createTime;
    Date updateTime;
    Date publishTime;
}
