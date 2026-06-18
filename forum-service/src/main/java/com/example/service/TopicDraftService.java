package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.TopicDraft;
import com.example.entity.vo.request.TopicDraftSaveVO;
import com.example.entity.vo.response.TopicDraftVO;

import java.util.List;

public interface TopicDraftService extends IService<TopicDraft> {
    List<TopicDraftVO> listDrafts(int userId);
    TopicDraftVO getDraft(int userId, int id);
    String saveDraft(int userId, TopicDraftSaveVO vo);
    void deleteDraft(int userId, int id);
}
