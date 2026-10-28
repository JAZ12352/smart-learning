package com.mindskip.xzs.controller;

import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.domain.UserEventLog;
import com.mindskip.xzs.domain.enums.RoleEnum;
import com.mindskip.xzs.domain.enums.UserStatusEnum;
import com.mindskip.xzs.event.UserEvent;
import com.mindskip.xzs.service.AuthenticationService;
import com.mindskip.xzs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.UUID;

/**
 * 公共注册控制器 — 学生和教师均可通过此接口注册
 */
@RestController
@RequestMapping("/api/user")
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationService authenticationService,
                          ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 通用注册 — 支持学生(1)和教师(2)
     */
    @PostMapping("/register")
    public RestResponse register(@RequestBody @Valid RegisterRequest request) {
        // 检查用户名是否已存在
        User existUser = userService.getUserByUserName(request.getUserName());
        if (existUser != null) {
            return new RestResponse<>(2, "用户名已存在");
        }

        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(authenticationService.pwdEncode(request.getPassword()));
        user.setUserUuid(UUID.randomUUID().toString());
        user.setRealName(request.getRealName() != null ? request.getRealName() : request.getUserName());
        user.setUserLevel(request.getUserLevel() != null ? request.getUserLevel() : 1);
        user.setRole(request.getRole() != null ? request.getRole() : RoleEnum.STUDENT.getCode());
        user.setStatus(UserStatusEnum.Enable.getCode());
        user.setCreateTime(new Date());
        user.setLastActiveTime(new Date());
        user.setDeleted(false);

        userService.insertByFilter(user);

        String roleName = user.getRole() == RoleEnum.TEACHER.getCode() ? "教师" : "学生";
        UserEventLog log = new UserEventLog(user.getId(), user.getUserName(), user.getRealName(), new Date());
        log.setContent(roleName + " " + user.getUserName() + " 通过注册加入平台");
        eventPublisher.publishEvent(new UserEvent(log));

        return RestResponse.ok();
    }

    /**
     * 注册请求体
     */
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String userName;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String realName;
        private Integer userLevel;
        private Integer role; // 1=学生, 2=教师

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public Integer getUserLevel() { return userLevel; }
        public void setUserLevel(Integer userLevel) { this.userLevel = userLevel; }
        public Integer getRole() { return role; }
        public void setRole(Integer role) { this.role = role; }
    }
}
