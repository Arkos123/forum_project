package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementDetailVO {
    Integer id;
    String title;
    String summary;
    String content;
    Boolean top;
    Date createTime;
    Date publishTime;
}
