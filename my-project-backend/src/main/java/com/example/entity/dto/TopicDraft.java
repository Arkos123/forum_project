package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_topic_draft")
public class TopicDraft implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer userId;
    Integer type;
    String title;
    String content;
    Date createTime;
    Date updateTime;
}
