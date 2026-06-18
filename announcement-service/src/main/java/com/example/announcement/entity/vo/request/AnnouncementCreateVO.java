package com.example.announcement.entity.vo.request;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class AnnouncementCreateVO {
    @NotBlank
    @Length(min = 1, max = 100)
    String title;
    @Length(max = 255)
    String summary;
    @NotNull
    JSONObject content;
}
