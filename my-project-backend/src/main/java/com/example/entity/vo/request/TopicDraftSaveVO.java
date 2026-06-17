package com.example.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TopicDraftSaveVO {
    @Min(1)
    Integer id;
    @Min(1)
    Integer type;
    @Length(max = 30)
    String title;
    JSONObject content;
}
