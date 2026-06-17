package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TopicDraftVO {
    Integer id;
    Integer type;
    String title;
    String content;
    Date createTime;
    Date updateTime;
}
