package com.mindskip.xzs.controller.admin;

import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.other.KeyValue;
import com.mindskip.xzs.domain.User;
import com.mindskip.xzs.domain.UserEventLog;
import com.mindskip.xzs.domain.enums.UserStatusEnum;
import com.mindskip.xzs.service.AuthenticationService;
import com.mindskip.xzs.service.SubjectService;
import com.mindskip.xzs.service.UserEventLogService;
import com.mindskip.xzs.service.UserService;
import com.mindskip.xzs.utility.DateTimeUtil;
import com.mindskip.xzs.viewmodel.admin.user.*;
import com.mindskip.xzs.utility.PageInfoHelper;
import com.github.pagehelper.PageInfo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController("AdminUserController")
@RequestMapping(value = "/api/admin/user")
public class UserController extends BaseApiController {

    private final UserService userService;
    private final UserEventLogService userEventLogService;
    private final AuthenticationService authenticationService;
    private final SubjectService subjectService;

    @Autowired
    public UserController(UserService userService, UserEventLogService userEventLogService,
                          AuthenticationService authenticationService, SubjectService subjectService) {
        this.userService = userService;
        this.userEventLogService = userEventLogService;
        this.authenticationService = authenticationService;
        this.subjectService = subjectService;
    }


    @RequestMapping(value = "/page/list", method = RequestMethod.POST)
    public RestResponse<PageInfo<UserResponseVM>> pageList(@RequestBody UserPageRequestVM model) {
        PageInfo<User> pageInfo = userService.userPage(model);
        PageInfo<UserResponseVM> page = PageInfoHelper.copyMap(pageInfo, d -> {
            UserResponseVM vm = UserResponseVM.from(d);
            if (d.getMajorId() != null) {
                com.mindskip.xzs.domain.Subject s = subjectService.selectById(d.getMajorId());
                if (s != null) vm.setMajorName(s.getName());
            }
            return vm;
        });
        return RestResponse.ok(page);
    }


    @RequestMapping(value = "/event/page/list", method = RequestMethod.POST)
    public RestResponse<PageInfo<UserEventLogVM>> eventPageList(@RequestBody UserEventPageRequestVM model) {
        PageInfo<UserEventLog> pageInfo = userEventLogService.page(model);
        PageInfo<UserEventLogVM> page = PageInfoHelper.copyMap(pageInfo, d -> {
            UserEventLogVM vm = modelMapper.map(d, UserEventLogVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(d.getCreateTime()));
            return vm;
        });
        return RestResponse.ok(page);
    }

    @RequestMapping(value = "/select/{id}", method = RequestMethod.POST)
    public RestResponse<UserResponseVM> select(@PathVariable String id) {
        User user = null;
        try {
            Integer userId = Integer.valueOf(id);
            user = userService.getUserById(userId);
        } catch (NumberFormatException e) {
            user = userService.getUserByUserName(id);
        }
        if (user == null) {
            return RestResponse.fail(2, "用户不存在");
        }
        UserResponseVM userVm = UserResponseVM.from(user);
        return RestResponse.ok(userVm);
    }

    @RequestMapping(value = "/current", method = RequestMethod.POST)
    public RestResponse<UserResponseVM> current() {
        User user = getCurrentUser();
        UserResponseVM userVm = UserResponseVM.from(user);
        return RestResponse.ok(userVm);
    }
//
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public RestResponse<User> edit(@RequestBody @Valid UserCreateVM model) {
        if (model.getId() == null) {  //create
            //sql语句根据delete=0和userName查询第一条用户
            User existUser = userService.getUserByUserName(model.getUserName());//从model获取一个userName
            if (null != existUser) {
                return new RestResponse<>(2, "用户已存在");
            }

            if (StringUtils.isBlank(model.getPassword())) {
                return new RestResponse<>(3, "密码不能为空");
            }
        }
        if (StringUtils.isBlank(model.getBirthDay())) {
            model.setBirthDay(null);
        }
        User user = modelMapper.map(model, User.class);

        if (model.getId() == null) {
            String encodePwd = authenticationService.pwdEncode(model.getPassword());
            user.setPassword(encodePwd);
            user.setUserUuid(UUID.randomUUID().toString());
            user.setCreateTime(new Date());
            user.setLastActiveTime(new Date());
            user.setDeleted(false);
            userService.insertByFilter(user);
        } else {
            if (!StringUtils.isBlank(model.getPassword())) {
                String encodePwd = authenticationService.pwdEncode(model.getPassword());
                user.setPassword(encodePwd);
            }
            user.setModifyTime(new Date());
            userService.updateByIdFilter(user);
        }
        return RestResponse.ok(user);
    }


    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public RestResponse update(@RequestBody @Valid UserUpdateVM model) {
        User user = userService.selectById(getCurrentUser().getId());
        modelMapper.map(model, user);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok();
    }

    //
    @RequestMapping(value = "/changeStatus/{id}", method = RequestMethod.POST)
    public RestResponse<Integer> changeStatus(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        UserStatusEnum userStatusEnum = UserStatusEnum.fromCode(user.getStatus());
        Integer newStatus = userStatusEnum == UserStatusEnum.Enable ? UserStatusEnum.Disable.getCode() : UserStatusEnum.Enable.getCode();
        user.setStatus(newStatus);
        user.setModifyTime(new Date());
        userService.updateByIdFilter(user);
        return RestResponse.ok(newStatus);
    }


    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    public RestResponse delete(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        user.setDeleted(true);
        userService.updateByIdFilter(user);
        return RestResponse.ok();
    }


    @RequestMapping(value = "/selectByUserName", method = RequestMethod.POST)
    public RestResponse<List<KeyValue>> selectByUserName(@RequestBody String userName) {
        List<KeyValue> keyValues = userService.selectByUserName(userName);
        return RestResponse.ok(keyValues);
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * 更新学生专业
     */
    @PostMapping("/major/update")
    public RestResponse<Void> updateMajor(@RequestBody Map<String, Object> body) {
        try {
            Integer userId = Integer.valueOf(String.valueOf(body.get("userId")));
            Object majorIdObj = body.get("majorId");
            if (majorIdObj != null) {
                Integer majorId = Integer.valueOf(String.valueOf(majorIdObj));
                User user = userService.getUserById(userId);
                if (user == null) return RestResponse.fail(2, "用户不存在");
                user.setMajorId(majorId);
                user.setModifyTime(new Date());
                userService.updateByIdFilter(user);
            } else {
                // 清除专业：直接用SQL设NULL (因updateByPrimaryKeySelective会跳过null字段)
                jdbcTemplate.update("UPDATE t_user SET major_id = NULL, modify_time = NOW() WHERE id = ?", userId);
            }
            return RestResponse.ok();
        } catch (Exception e) {
            return RestResponse.fail(2, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 按专业获取学生列表
     */
    @GetMapping("/list-by-major/{majorId}")
    public RestResponse<List<UserResponseVM>> listByMajor(@PathVariable Integer majorId) {
        List<User> users = userService.getUserByMajorId(majorId);
        List<UserResponseVM> list = users.stream().map(d -> {
            UserResponseVM vm = UserResponseVM.from(d);
            com.mindskip.xzs.domain.Subject s = subjectService.selectById(d.getMajorId());
            if (s != null) vm.setMajorName(s.getName());
            return vm;
        }).collect(java.util.stream.Collectors.toList());
        return RestResponse.ok(list);
    }

    /**
     * 获取所有学生（角色=1）
     */
    @GetMapping("/all-students")
    public RestResponse<List<UserResponseVM>> allStudents() {
        List<User> users = userService.getUserByRole(1);
        List<UserResponseVM> list = users.stream().map(d -> {
            UserResponseVM vm = UserResponseVM.from(d);
            if (d.getMajorId() != null) {
                com.mindskip.xzs.domain.Subject s = subjectService.selectById(d.getMajorId());
                if (s != null) vm.setMajorName(s.getName());
            }
            return vm;
        }).collect(java.util.stream.Collectors.toList());
        return RestResponse.ok(list);
    }

}
