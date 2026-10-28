package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.LearningPath;
import com.mindskip.xzs.repository.LearningPathMapper;
import com.mindskip.xzs.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningPathServiceImpl extends BaseServiceImpl<LearningPath> implements LearningPathService {

    private final LearningPathMapper learningPathMapper;

    @Autowired
    public LearningPathServiceImpl(LearningPathMapper learningPathMapper) {
        super(learningPathMapper);
        this.learningPathMapper = learningPathMapper;
    }

    @Override
    public List<LearningPath> getPathsByUserId(Integer userId) {
        return learningPathMapper.selectByUserId(userId);
    }

    @Override
    public LearningPath getPathByUserAndSubject(Integer userId, Integer subjectId) {
        return learningPathMapper.selectByUserAndSubject(userId, subjectId);
    }
}
