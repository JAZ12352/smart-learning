package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.GeneratedResource;
import com.mindskip.xzs.domain.SubjectResource;
import com.mindskip.xzs.repository.GeneratedResourceMapper;
import com.mindskip.xzs.repository.SubjectResourceMapper;
import com.mindskip.xzs.service.SubjectResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SubjectResourceServiceImpl extends BaseServiceImpl<SubjectResource> implements SubjectResourceService {

    private final SubjectResourceMapper subjectResourceMapper;
    private final GeneratedResourceMapper generatedResourceMapper;

    @Autowired
    public SubjectResourceServiceImpl(SubjectResourceMapper subjectResourceMapper,
                                      GeneratedResourceMapper generatedResourceMapper) {
        super(subjectResourceMapper);
        this.subjectResourceMapper = subjectResourceMapper;
        this.generatedResourceMapper = generatedResourceMapper;
    }

    @Override
    public List<SubjectResource> getResourcesBySubjectId(Integer subjectId) {
        return subjectResourceMapper.selectBySubjectId(subjectId);
    }

    @Override
    public int incrementClickCount(Integer id) {
        return subjectResourceMapper.incrementClickCount(id);
    }

    @Override
    public int syncFromGeneratedResource(Integer subjectId) {
        // 查询该学科下的所有生成资源
        List<GeneratedResource> generatedList = generatedResourceMapper.selectBySubjectId(subjectId);

        int count = 0;
        for (GeneratedResource gr : generatedList) {
            // 检查是否已同步
            List<SubjectResource> existing = subjectResourceMapper.selectBySubjectId(subjectId);
            boolean exists = existing.stream()
                    .anyMatch(sr -> "generated".equals(sr.getSourceType())
                            && sr.getSourceId() != null
                            && sr.getSourceId().equals(gr.getId()));
            if (exists) {
                continue;
            }

            SubjectResource sr = new SubjectResource();
            sr.setSubjectId(subjectId);
            sr.setSourceType("generated");
            sr.setSourceId(gr.getId());
            sr.setTitle(gr.getTitle());
            sr.setSummary(gr.getTitle());
            sr.setSource(gr.getSourceAgent());
            sr.setContent(gr.getContent());
            // 资源类型标记：mind_map → format=mind_map，方便前端用 Markmap 渲染
            String fmt = gr.getContentFormat() != null ? gr.getContentFormat() : "markdown";
            if ("mind_map".equals(gr.getResourceType())) {
                fmt = "mind_map";
            }
            sr.setFormat(fmt);
            sr.setClickCount(0);
            sr.setCreateTime(new Date());
            sr.setDeleted(false);
            subjectResourceMapper.insert(sr);
            count++;
        }
        return count;
    }
}
