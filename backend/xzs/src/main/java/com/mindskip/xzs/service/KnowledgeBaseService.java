package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.KbDocument;
import com.mindskip.xzs.domain.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends BaseService<KnowledgeBase> {

    List<KnowledgeBase> getAllKnowledgeBases();

    List<KbDocument> getDocumentsByKbId(Integer kbId);

    KbDocument getDocumentById(Integer docId);

    int deleteDocument(Integer docId);

    int countDocuments(Integer kbId);

    int insertDocument(KbDocument document);

    int updateDocument(KbDocument document);
}
