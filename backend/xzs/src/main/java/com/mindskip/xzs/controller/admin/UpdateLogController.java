package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.SystemCode;
import com.mindskip.xzs.configuration.spring.security.RestUtil;
import com.mindskip.xzs.service.impl.UserEventLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping(value = "/api/admin/update-log")
public class UpdateLogController {

    private final UserEventLogServiceImpl userEventLogService;

    @Autowired
    public UpdateLogController(UserEventLogServiceImpl userEventLogService) {
        this.userEventLogService = userEventLogService;
    }

    @GetMapping(value = "/update-login-content")
    public void updateLoginContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 执行更新操作
            int updatedCount = userEventLogService.updateLoginContent();
            RestUtil.response(response, SystemCode.OK.getCode(), "更新成功，共更新了" + updatedCount + "条记录", null);
        } catch (Exception e) {
            RestUtil.response(response, SystemCode.InnerError.getCode(), "更新失败：" + e.getMessage(), null);
        }
    }
}
