package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.SubjectResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubjectResourceMapper extends BaseMapper<SubjectResource> {

    List<SubjectResource> selectBySubjectId(@Param("subjectId") Integer subjectId);

    int incrementClickCount(@Param("id") Integer id);
}
