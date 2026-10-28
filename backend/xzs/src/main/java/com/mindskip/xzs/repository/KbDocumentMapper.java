package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    List<KbDocument> selectByKbId(@Param("kbId") Integer kbId);

    int countByKbId(@Param("kbId") Integer kbId);
}
