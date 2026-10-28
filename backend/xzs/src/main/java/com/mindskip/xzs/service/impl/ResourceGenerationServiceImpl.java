package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.GeneratedResource;
import com.mindskip.xzs.repository.GeneratedResourceMapper;
import com.mindskip.xzs.service.ResourceGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceGenerationServiceImpl extends BaseServiceImpl<GeneratedResource> implements ResourceGenerationService {

    private final GeneratedResourceMapper generatedResourceMapper;

    @Autowired
    public ResourceGenerationServiceImpl(GeneratedResourceMapper generatedResourceMapper) {
        super(generatedResourceMapper);
        this.generatedResourceMapper = generatedResourceMapper;
    }

    @Override
    public List<GeneratedResource> getResourcesByUserId(Integer userId) {
        return generatedResourceMapper.selectByUserId(userId);
    }

    @Override
    public List<GeneratedResource> getResourcesByUserAndType(Integer userId, String resourceType) {
        return generatedResourceMapper.selectByUserAndType(userId, resourceType);
    }

    @Override
    public int deleteByUserId(Integer userId) {
        return generatedResourceMapper.deleteByUserId(userId);
    }
}
