package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningPathMapper extends BaseMapper<LearningPath> {

    /**
     * selectByUserId
     *
     * @param userId userId
     * @return List<LearningPath>
     */
    List<LearningPath> selectByUserId(@Param("userId") Integer userId);

    /**
     * selectByUserAndSubject
     *
     * @param userId    userId
     * @param subjectId subjectId
     * @return LearningPath
     */
    LearningPath selectByUserAndSubject(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);
}
