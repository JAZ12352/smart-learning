package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.StudentProfile;

public interface StudentProfileService extends BaseService<StudentProfile> {

    StudentProfile getProfileByUserId(Integer userId);

    void updateProfile(Integer userId, StudentProfile profile);
}
