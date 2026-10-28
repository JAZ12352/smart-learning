package com.mindskip.xzs.service;

import com.mindskip.xzs.domain.DialogueSession;

import java.util.List;

public interface DialogueSessionService extends BaseService<DialogueSession> {

    List<DialogueSession> getSessionsByUserId(Integer userId);

    DialogueSession getActiveSession(Integer userId, String sessionType);
}
