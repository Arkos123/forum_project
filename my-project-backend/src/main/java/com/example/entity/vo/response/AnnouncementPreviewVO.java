package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementPreviewVO {
    Integer id;
    String title;
    String summary;
    Boolean top;
    Date createTime;
    Date publishTime;
}
