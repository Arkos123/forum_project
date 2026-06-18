package com.example.announcement.entity.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnouncementPublishVO {
    @NotNull
    @Min(1)
    Integer id;
    @NotNull
    Boolean published;
}
