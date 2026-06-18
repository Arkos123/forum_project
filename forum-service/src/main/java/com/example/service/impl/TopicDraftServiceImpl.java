package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.TopicDraft;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.TopicDraftSaveVO;
import com.example.entity.vo.response.TopicDraftVO;
import com.example.mapper.TopicDraftMapper;
import com.example.mapper.TopicTypeMapper;
import com.example.service.TopicDraftService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TopicDraftServiceImpl extends ServiceImpl<TopicDraftMapper, TopicDraft> implements TopicDraftService {

    @Resource
    TopicTypeMapper topicTypeMapper;

    private Set<Integer> types;

    @PostConstruct
    private void initTypes() {
        types = topicTypeMapper.selectList(null)
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TopicDraftVO> listDrafts(int userId) {
        return baseMapper.selectList(Wrappers.<TopicDraft>query()
                        .eq("user_id", userId)
                        .orderByDesc("update_time"))
                .stream()
                .map(this::asViewObject)
                .toList();
    }

    @Override
    public TopicDraftVO getDraft(int userId, int id) {
        TopicDraft draft = baseMapper.selectOne(Wrappers.<TopicDraft>query()
                .eq("id", id)
                .eq("user_id", userId));
        return draft == null ? null : asViewObject(draft);
    }

    @Override
    public String saveDraft(int userId, TopicDraftSaveVO vo) {
        if(vo.getType() != null && !types.contains(vo.getType()))
            return "文章类型非法！";
        if(!contentLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，草稿保存失败！";
        Date now = new Date();
        TopicDraft draft;
        if(vo.getId() != null) {
            draft = baseMapper.selectOne(Wrappers.<TopicDraft>query()
                    .eq("id", vo.getId())
                    .eq("user_id", userId));
            if(draft == null)
                return "草稿不存在或无权修改";
        } else {
            draft = new TopicDraft();
            draft.setUserId(userId);
            draft.setCreateTime(now);
        }
        draft.setType(vo.getType());
        draft.setTitle(vo.getTitle());
        draft.setContent(vo.getContent() == null ? null : vo.getContent().toJSONString());
        draft.setUpdateTime(now);
        if(this.saveOrUpdate(draft)) {
            vo.setId(draft.getId());
            return null;
        }
        return "草稿保存失败，请联系管理员！";
    }

    @Override
    public void deleteDraft(int userId, int id) {
        baseMapper.delete(Wrappers.<TopicDraft>query()
                .eq("id", id)
                .eq("user_id", userId));
    }

    private TopicDraftVO asViewObject(TopicDraft draft) {
        TopicDraftVO vo = new TopicDraftVO();
        BeanUtils.copyProperties(draft, vo);
        return vo;
    }

    private boolean contentLimitCheck(JSONObject object, int max) {
        if(object == null) return true;
        JSONArray ops = object.getJSONArray("ops");
        if(ops == null) return false;
        long length = 0;
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if(insert instanceof String text) {
                length += text.length();
                if(length > max) return false;
            }
        }
        return true;
    }
}
