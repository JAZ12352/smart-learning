package com.mindskip.xzs.service.impl;

import com.mindskip.xzs.domain.DialogueSession;
import com.mindskip.xzs.repository.DialogueSessionMapper;
import com.mindskip.xzs.service.DialogueSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DialogueSessionServiceImpl extends BaseServiceImpl<DialogueSession> implements DialogueSessionService {

    private final DialogueSessionMapper dialogueSessionMapper;

    @Autowired
    public DialogueSessionServiceImpl(DialogueSessionMapper dialogueSessionMapper) {
        super(dialogueSessionMapper);
        this.dialogueSessionMapper = dialogueSessionMapper;
    }

    @Override
    public List<DialogueSession> getSessionsByUserId(Integer userId) {
        return dialogueSessionMapper.selectByUserId(userId);
    }

    @Override
    public DialogueSession getActiveSession(Integer userId, String sessionType) {
        return dialogueSessionMapper.selectActiveByUserId(userId, sessionType);
    }
}
