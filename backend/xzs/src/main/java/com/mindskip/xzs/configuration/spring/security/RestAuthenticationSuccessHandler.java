package com.mindskip.xzs.configuration.spring.security;

import com.mindskip.xzs.base.SystemCode;
import com.mindskip.xzs.domain.UserEventLog;
import com.mindskip.xzs.event.UserEvent;
import com.mindskip.xzs.service.UserService;
import com.mindskip.xzs.service.UserTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;


/**
 * @version 3.5.0
 * @description: 登录成功返回
 * Copyright (C), 2020-2025, 智慧乐学
 * @date 2021/12/25 9:45
 */
@Component
public class RestAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final UserTokenService userTokenService;

    /**
     * Instantiates a new Rest authentication success handler.
     *
     * @param eventPublisher the event publisher
     * @param userService    the user service
     * @param userTokenService the user token service
     */
    @Autowired
    public RestAuthenticationSuccessHandler(ApplicationEventPublisher eventPublisher, UserService userService, UserTokenService userTokenService) {
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.userTokenService = userTokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object object = authentication.getPrincipal();
        if (null != object) {
            User springUser = (User) object;
            com.mindskip.xzs.domain.User user = userService.getUserByUserName(springUser.getUsername());
            if (null != user) {
                UserEventLog userEventLog = new UserEventLog(user.getId(), user.getUserName(), user.getRealName(), new Date());
                userEventLog.setContent(user.getUserName() + " 登录了智能教师管理系统");
                eventPublisher.publishEvent(new UserEvent(userEventLog));
                
                // 生成UserToken并返回给前端
                com.mindskip.xzs.domain.UserToken userToken = userTokenService.insertUserToken(user);
                
                com.mindskip.xzs.domain.User newUser = new com.mindskip.xzs.domain.User();
                newUser.setUserName(user.getUserName());
                newUser.setImagePath(user.getImagePath());
                
                // 将token添加到返回结果中
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("user", newUser);
                result.put("token", userToken.getToken());
                
                RestUtil.response(response, SystemCode.OK.getCode(), SystemCode.OK.getMessage(), result);
            }
        } else {
            RestUtil.response(response, SystemCode.UNAUTHORIZED.getCode(), SystemCode.UNAUTHORIZED.getMessage());
        }
    }
}
