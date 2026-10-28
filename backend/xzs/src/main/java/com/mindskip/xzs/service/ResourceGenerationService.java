package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.GeneratedResource;

import java.util.List;

public interface ResourceGenerationService extends BaseService<GeneratedResource> {

    List<GeneratedResource> getResourcesByUserId(Integer userId);

    List<GeneratedResource> getResourcesByUserAndType(Integer userId, String resourceType);

    int deleteByUserId(Integer userId);
}
