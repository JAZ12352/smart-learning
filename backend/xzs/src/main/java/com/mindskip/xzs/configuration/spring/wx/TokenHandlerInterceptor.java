package com.mindskip.xzs.configuration.spring.wx;

import com.mindskip.xzs.base.SystemCode;
import com.mindskip.xzs.configuration.spring.security.RestUtil;
import com.mindskip.xzs.context.WxContext;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.domain.UserToken;
import com.mindskip.xzs.service.UserService;
import com.mindskip.xzs.service.UserTokenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class TokenHandlerInterceptor implements HandlerInterceptor {

    private final UserTokenService userTokenService;
    private final UserService userService;
    private final WxContext wxContext;

    @Autowired
    public TokenHandlerInterceptor(UserTokenService userTokenService, UserService userService, WxContext wxContext) {
        this.userTokenService = userTokenService;
        this.userService = userService;
        this.wxContext = wxContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        // 尝试从 Authorization 头中获取 token
        if (StringUtils.isEmpty(token)) {
            String authorization = request.getHeader("Authorization");
            if (StringUtils.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
        }
        if (StringUtils.isEmpty(token)) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }

        if (StringUtils.isBlank(token)) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }

        if (token.length() != 36) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }

        UserToken userToken = userTokenService.getToken(token);
        if (null == userToken) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }

        Date now = new Date();
        User user = userService.getUserByUserName(userToken.getUserName());
        if (user == null) {
            RestUtil.response(response, SystemCode.UNAUTHORIZED);
            return false;
        }
        if (now.before(userToken.getEndTime())) {
            try {
                wxContext.setContext(user,userToken);
            } catch (Exception e) {
                // 忽略 RequestContextHolder 异常，继续执行
                System.out.println("RequestContextHolder exception: " + e.getMessage());
            }
            return true;
        } else {   //refresh token
            try {
                UserToken refreshToken = userTokenService.insertUserToken(user);
                RestUtil.response(response, SystemCode.AccessTokenError.getCode(), SystemCode.AccessTokenError.getMessage(), refreshToken.getToken());
            } catch (Exception e) {
                // 忽略异常，返回未授权
                RestUtil.response(response, SystemCode.UNAUTHORIZED);
            }
            return false;
        }
    }
}
