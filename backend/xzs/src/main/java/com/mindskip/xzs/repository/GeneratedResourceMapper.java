package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.GeneratedResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeneratedResourceMapper extends BaseMapper<GeneratedResource> {

    /**
     * selectByUserId
     *
     * @param userId userId
     * @return List<GeneratedResource>
     */
    List<GeneratedResource> selectByUserId(@Param("userId") Integer userId);

    /**
     * selectByUserAndType
     *
     * @param userId       userId
     * @param resourceType resourceType
     * @return List<GeneratedResource>
     */
    List<GeneratedResource> selectByUserAndType(@Param("userId") Integer userId, @Param("resourceType") String resourceType);

    /**
     * selectBySubjectId
     *
     * @param subjectId subjectId
     * @return List<GeneratedResource>
     */
    List<GeneratedResource> selectBySubjectId(@Param("subjectId") Integer subjectId);

    /**
     * deleteByUserId
     *
     * @param userId userId
     * @return int
     */
    int deleteByUserId(@Param("userId") Integer userId);
}
