package com.mindskip.xzs.repository;

import com.mindskip.xzs.domain.DialogueSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DialogueSessionMapper extends BaseMapper<DialogueSession> {

    /**
     * selectByUserId
     *
     * @param userId userId
     * @return List<DialogueSession>
     */
    List<DialogueSession> selectByUserId(@Param("userId") Integer userId);

    /**
     * selectActiveByUserId
     *
     * @param userId      userId
     * @param sessionType sessionType
     * @return DialogueSession
     */
    DialogueSession selectActiveByUserId(@Param("userId") Integer userId, @Param("sessionType") String sessionType);
}
