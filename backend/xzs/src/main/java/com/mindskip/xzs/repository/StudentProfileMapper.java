package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {

    /**
     * selectByUserId
     *
     * @param userId userId
     * @return StudentProfile
     */
    StudentProfile selectByUserId(@Param("userId") Integer userId);
}
