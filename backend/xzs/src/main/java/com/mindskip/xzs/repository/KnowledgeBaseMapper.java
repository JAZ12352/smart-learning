package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    List<KnowledgeBase> selectAll();
}
