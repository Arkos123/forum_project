package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_announcement")
public class Announcement implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer uid;
    String title;
    String summary;
    String content;
    Boolean published;
    Boolean top;
    Date createTime;
    Date updateTime;
    Date publishTime;
}
