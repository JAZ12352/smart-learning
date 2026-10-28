package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.LearningPath;

import java.util.List;

public interface LearningPathService extends BaseService<LearningPath> {

    List<LearningPath> getPathsByUserId(Integer userId);

    LearningPath getPathByUserAndSubject(Integer userId, Integer subjectId);
}
