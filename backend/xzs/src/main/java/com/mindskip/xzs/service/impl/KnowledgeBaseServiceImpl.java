package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.KbDocument;
import com.mindskip.xzs.domain.KnowledgeBase;
import com.mindskip.xzs.repository.KbDocumentMapper;
import com.mindskip.xzs.repository.KnowledgeBaseMapper;
import com.mindskip.xzs.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends BaseServiceImpl<KnowledgeBase> implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KbDocumentMapper kbDocumentMapper;

    @Autowired
    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper, KbDocumentMapper kbDocumentMapper) {
        super(knowledgeBaseMapper);
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.kbDocumentMapper = kbDocumentMapper;
    }

    @Override
    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseMapper.selectAll();
    }

    @Override
    public List<KbDocument> getDocumentsByKbId(Integer kbId) {
        return kbDocumentMapper.selectByKbId(kbId);
    }

    @Override
    public KbDocument getDocumentById(Integer docId) {
        return kbDocumentMapper.selectByPrimaryKey(docId);
    }

    @Override
    public int deleteDocument(Integer docId) {
        return kbDocumentMapper.deleteByPrimaryKey(docId);
    }

    @Override
    public int countDocuments(Integer kbId) {
        return kbDocumentMapper.countByKbId(kbId);
    }

    @Override
    public int insertDocument(KbDocument document) {
        return kbDocumentMapper.insert(document);
    }

    @Override
    public int updateDocument(KbDocument document) {
        return kbDocumentMapper.updateByPrimaryKeySelective(document);
    }
}
