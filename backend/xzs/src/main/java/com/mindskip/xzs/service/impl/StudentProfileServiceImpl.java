package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.StudentProfile;
import com.mindskip.xzs.repository.StudentProfileMapper;
import com.mindskip.xzs.service.StudentProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class StudentProfileServiceImpl extends BaseServiceImpl<StudentProfile> implements StudentProfileService {

    private final StudentProfileMapper studentProfileMapper;

    @Autowired
    public StudentProfileServiceImpl(StudentProfileMapper studentProfileMapper) {
        super(studentProfileMapper);
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    public StudentProfile getProfileByUserId(Integer userId) {
        StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setProfileVersion(1);
            profile.setCreateTime(new Date());
            profile.setModifyTime(new Date());
            profile.setDeleted(false);
            studentProfileMapper.insertSelective(profile);
        }
        return profile;
    }

    @Override
    public void updateProfile(Integer userId, StudentProfile profile) {
        StudentProfile existing = studentProfileMapper.selectByUserId(userId);
        if (existing != null) {
            profile.setId(existing.getId());
            profile.setUserId(userId);
            profile.setModifyTime(new Date());
            studentProfileMapper.updateByPrimaryKeySelective(profile);
        } else {
            profile.setUserId(userId);
            profile.setCreateTime(new Date());
            profile.setModifyTime(new Date());
            profile.setDeleted(false);
            studentProfileMapper.insertSelective(profile);
        }
    }
}
