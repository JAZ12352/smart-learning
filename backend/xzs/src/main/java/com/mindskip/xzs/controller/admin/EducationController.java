package com.mindskip.xzs.controller.admin;


import com.mindskip.xzs.base.BaseApiController;
import com.mindskip.xzs.base.RestResponse;
import com.mindskip.xzs.domain.Subject;
import com.mindskip.xzs.domain.SubjectResource;
import com.mindskip.xzs.service.SubjectCatalogService;
import com.mindskip.xzs.service.SubjectResourceService;
import com.mindskip.xzs.service.SubjectService;
import com.mindskip.xzs.utility.PageInfoHelper;
import com.mindskip.xzs.viewmodel.admin.education.SubjectEditRequestVM;
import com.mindskip.xzs.viewmodel.admin.education.SubjectPageRequestVM;
import com.mindskip.xzs.viewmodel.admin.education.SubjectResponseVM;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController("AdminEducationController")
@RequestMapping(value = "/api/admin/education")
public class EducationController extends BaseApiController {

    private final SubjectService subjectService;
    private final SubjectResourceService subjectResourceService;
    private final SubjectCatalogService subjectCatalogService;

    @Autowired
    public EducationController(SubjectService subjectService,
                               SubjectResourceService subjectResourceService,
                               SubjectCatalogService subjectCatalogService) {
        this.subjectService = subjectService;
        this.subjectResourceService = subjectResourceService;
        this.subjectCatalogService = subjectCatalogService;
    }

    @RequestMapping(value = "/subject/list", method = RequestMethod.POST)
    public RestResponse<List<Subject>> list() {
        List<Subject> subjects = subjectService.allSubject();
        return RestResponse.ok(subjects);
    }

    @RequestMapping(value = "/subject/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<SubjectResponseVM>> pageList(@RequestBody SubjectPageRequestVM model) {
        PageInfo<Subject> pageInfo = subjectService.page(model);
        PageInfo<SubjectResponseVM> page = PageInfoHelper.copyMap(pageInfo, e -> modelMapper.map(e, SubjectResponseVM.class));
        return RestResponse.ok(page);
    }

    @RequestMapping(value = "/subject/edit", method = RequestMethod.POST)
    public RestResponse edit(@RequestBody @Valid SubjectEditRequestVM model) {
        Subject subject = modelMapper.map(model, Subject.class);
        if (model.getId() == null) {
            subject.setDeleted(false);
            subjectService.insertByFilter(subject);
        } else {
            subjectService.updateByIdFilter(subject);
        }
        return RestResponse.ok();
    }

    @RequestMapping(value = "/subject/select/{id}", method = RequestMethod.POST)
    public RestResponse<SubjectEditRequestVM> select(@PathVariable Integer id) {
        Subject subject = subjectService.selectById(id);
        SubjectEditRequestVM vm = modelMapper.map(subject, SubjectEditRequestVM.class);
        return RestResponse.ok(vm);
    }

    @RequestMapping(value = "/subject/delete/{id}", method = RequestMethod.POST)
    public RestResponse delete(@PathVariable Integer id) {
        Subject subject = subjectService.selectById(id);
        subject.setDeleted(true);
        subjectService.updateByIdFilter(subject);
        return RestResponse.ok();
    }

    /**
     * 按学历层次获取学科列表
     */
    @RequestMapping(value = "/subject/by-education-level", method = RequestMethod.POST)
    public RestResponse<List<Subject>> getByEducationLevel(@RequestBody SubjectPageRequestVM model) {
        List<Subject> subjects = subjectService.getSubjectByEducationLevel(model.getEducationLevel());
        return RestResponse.ok(subjects);
    }

    /**
     * 获取学科树（门类→一级学科→专业→课程）
     */
    @GetMapping("/subject/tree")
    public RestResponse<List<Map<String, Object>>> getSubjectTree(
            @RequestParam(required = false) Integer educationLevel) {
        return RestResponse.ok(subjectService.getSubjectTree(educationLevel));
    }

    /**
     * 按父级ID获取子学科
     */
    @GetMapping("/subject/by-parent/{parentId}")
    public RestResponse<List<Subject>> getByParentId(@PathVariable Integer parentId) {
        return RestResponse.ok(subjectService.getByParentId(parentId));
    }

    /**
     * 按学科类型获取（1门类/2一级学科/3专业/4课程）
     */
    @GetMapping("/subject/by-type/{subjectType}")
    public RestResponse<List<Subject>> getBySubjectType(@PathVariable Integer subjectType) {
        return RestResponse.ok(subjectService.getBySubjectType(subjectType));
    }

    // ── 学科资源管理 ──

    /**
     * 获取指定学科下的所有资源
     */
    @GetMapping("/subject/{subjectId}/resources")
    public RestResponse<List<SubjectResource>> getSubjectResources(@PathVariable Integer subjectId) {
        List<SubjectResource> resources = subjectResourceService.getResourcesBySubjectId(subjectId);
        return RestResponse.ok(resources);
    }

    /**
     * 获取单个资源详情（含完整内容）
     */
    @GetMapping("/subject/resource/{resourceId}")
    public RestResponse<SubjectResource> getResourceDetail(@PathVariable Integer resourceId) {
        SubjectResource resource = subjectResourceService.selectById(resourceId);
        if (resource == null || resource.getDeleted()) {
            return RestResponse.fail(2, "资源不存在");
        }
        // 增加点击计数
        subjectResourceService.incrementClickCount(resourceId);
        return RestResponse.ok(resource);
    }

    /**
     * 添加资源到学科
     */
    @PostMapping("/subject/{subjectId}/resource/add")
    public RestResponse<SubjectResource> addResource(@PathVariable Integer subjectId,
                                                     @RequestBody SubjectResource resource) {
        resource.setSubjectId(subjectId);
        resource.setClickCount(0);
        subjectResourceService.insert(resource);
        return RestResponse.ok(resource);
    }

    /**
     * 删除学科资源
     */
    @DeleteMapping("/subject/resource/{resourceId}")
    public RestResponse<Void> deleteResource(@PathVariable Integer resourceId) {
        subjectResourceService.deleteById(resourceId);
        return RestResponse.ok();
    }

    /**
     * 同步生成资源到学科
     */
    @PostMapping("/subject/{subjectId}/sync-resources")
    public RestResponse<Integer> syncGeneratedResources(@PathVariable Integer subjectId) {
        int count = subjectResourceService.syncFromGeneratedResource(subjectId);
        return RestResponse.ok(count);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/init")
    public RestResponse<String> initDatabase() {
        try {
            // 1. 清理K12旧数据
            int d = jdbcTemplate.update("UPDATE t_subject SET deleted = 1 WHERE deleted = 0 AND (education_level IS NULL OR (level BETWEEN 1 AND 12 AND subject_type IS NULL))");

            // 2. 检查是否有门类数据
            Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_subject WHERE deleted = 0 AND subject_type = 1", Integer.class);

            String msg = "已清理 " + d + " 条旧数据。";
            if (cnt == null || cnt == 0) {
                // 3. 导入完整教育部本科专业目录
                Map<String, Integer> stats = subjectCatalogService.importCatalog(2);
                msg += "已导入完整本科专业目录：" +
                    stats.get("categories") + " 个学科门类、" +
                    stats.get("groups") + " 个专业类、" +
                    stats.get("majors") + " 个专业。";
            } else {
                msg += "学科数据已存在（" + cnt + " 个门类），如需重新导入请先手动清空数据。";
            }
            return RestResponse.ok(msg);
        } catch (Exception e) {
            return RestResponse.fail(2, "初始化失败: " + e.getMessage());
        }
    }

    /**
     * 获取学科目录统计信息
     */
    @GetMapping("/catalog/stats")
    public RestResponse<Map<String, Integer>> getCatalogStats() {
        return RestResponse.ok(subjectCatalogService.getCatalogStats());
    }

    /**
     * 一键导入完整本科专业目录
     */
    @PostMapping("/catalog/import")
    public RestResponse<String> importCatalog(@RequestParam(defaultValue = "2") Integer educationLevel) {
        try {
            Map<String, Integer> stats = subjectCatalogService.importCatalog(educationLevel);
            return RestResponse.ok("导入完成：" +
                stats.get("categories") + " 个学科门类、" +
                stats.get("groups") + " 个专业类、" +
                stats.get("majors") + " 个专业");
        } catch (Exception e) {
            return RestResponse.fail(2, "导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定专业下所有课程 (subject_type=4)
     */
    @GetMapping("/major/{majorId}/courses")
    public RestResponse<List<Subject>> getMajorCourses(@PathVariable Integer majorId) {
        List<Subject> courses = subjectService.getByParentId(majorId);
        return RestResponse.ok(courses);
    }

    /**
     * 为专业添加一门课程
     */
    @PostMapping("/major/{majorId}/course/add")
    public RestResponse<Subject> addMajorCourse(@PathVariable Integer majorId, @RequestBody Subject course) {
        course.setParentId(majorId);
        course.setSubjectType(4);
        course.setEducationLevel(2);
        course.setEducationLevelName("本科");
        course.setDeleted(false);
        subjectService.insertByFilter(course);
        return RestResponse.ok(course);
    }

    /**
     * 删除专业下的一门课程
     */
    @DeleteMapping("/major/course/{courseId}")
    public RestResponse<Void> deleteMajorCourse(@PathVariable Integer courseId) {
        subjectService.deleteById(courseId);
        return RestResponse.ok();
    }

    /**
     * 为专业批量初始化推荐课程（AI生成内置课程模板）
     */
    @PostMapping("/major/{majorId}/init-courses")
    public RestResponse<Integer> initMajorCourses(@PathVariable Integer majorId) {
        Subject major = subjectService.selectById(majorId);
        if (major == null) return RestResponse.fail(2, "专业不存在");
        String majorName = major.getName();
        String category = major.getCategory();

        // 根据专业名推荐内置课程模板
        List<String> courseNames = generateRecommendedCourses(majorName, category);
        int count = 0;
        int order = 0;
        for (String cn : courseNames) {
            Subject course = new Subject();
            course.setName(cn);
            course.setParentId(majorId);
            course.setSubjectType(4);
            course.setEducationLevel(2);
            course.setEducationLevelName("本科");
            course.setCategory(category);
            course.setItemOrder(order++);
            course.setDeleted(false);
            subjectService.insertByFilter(course);
            count++;
        }
        return RestResponse.ok(count);
    }

    /**
     * 为所有无课程的专业批量初始化推荐课程
     */
    @PostMapping("/major/init-all-courses")
    public RestResponse<Map<String, Integer>> initAllMajorCourses() {
        List<Subject> allMajors = subjectService.getBySubjectType(3);
        int total = 0, majorCount = 0;
        for (Subject major : allMajors) {
            List<Subject> existing = subjectService.getByParentId(major.getId());
            if (existing != null && !existing.isEmpty()) continue; // 已有课程跳过
            List<String> courseNames = generateRecommendedCourses(major.getName(), major.getCategory());
            int order = 0;
            for (String cn : courseNames) {
                Subject course = new Subject();
                course.setName(cn);
                course.setParentId(major.getId());
                course.setSubjectType(4);
                course.setEducationLevel(major.getEducationLevel() != null ? major.getEducationLevel() : 2);
                course.setEducationLevelName("本科");
                course.setCategory(major.getCategory());
                course.setItemOrder(order++);
                course.setDeleted(false);
                subjectService.insertByFilter(course);
                total++;
            }
            majorCount++;
        }
        Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        stats.put("majors", majorCount);
        stats.put("courses", total);
        return RestResponse.ok(stats);
    }

    private List<String> generateRecommendedCourses(String majorName, String category) {
        java.util.Map<String, List<String>> templates = new java.util.LinkedHashMap<>();
        templates.put("软件工程", Arrays.asList("高等数学", "线性代数", "C语言程序设计", "数据结构", "面向对象程序设计", "数据库原理", "操作系统", "计算机网络", "软件工程导论", "毕业设计"));
        templates.put("计算机科学与技术", Arrays.asList("高等数学", "线性代数", "C语言程序设计", "数据结构", "计算机组成原理", "操作系统", "计算机网络", "数据库系统概论", "编译原理", "软件工程"));
        templates.put("数据科学与大数据技术", Arrays.asList("高等数学", "线性代数", "Python程序设计", "数据结构", "数据库原理", "机器学习", "大数据技术", "数据挖掘", "云计算", "毕业设计"));
        templates.put("网络工程", Arrays.asList("高等数学", "线性代数", "C语言程序设计", "数据结构", "计算机网络", "网络协议分析", "网络安全", "网络编程", "路由与交换", "毕业设计"));
        templates.put("信息安全", Arrays.asList("高等数学", "线性代数", "C语言程序设计", "数据结构", "计算机网络", "密码学", "网络攻防", "系统安全", "数据库安全", "毕业设计"));
        templates.put("人工智能", Arrays.asList("高等数学", "线性代数", "Python程序设计", "数据结构", "机器学习", "深度学习", "自然语言处理", "计算机视觉", "强化学习", "毕业设计"));
        templates.put("物联网工程", Arrays.asList("高等数学", "线性代数", "C语言程序设计", "数据结构", "计算机网络", "嵌入式系统", "传感器技术", "无线网络", "物联网安全", "毕业设计"));
        templates.put("会计学", Arrays.asList("微积分", "线性代数", "会计学基础", "中级财务会计", "成本会计", "财务管理", "审计学", "税法", "经济法", "毕业设计"));
        templates.put("财务管理", Arrays.asList("微积分", "线性代数", "会计学基础", "财务管理", "中级财务管理", "投资学", "财务报表分析", "公司理财", "管理会计", "毕业设计"));
        templates.put("市场营销", Arrays.asList("微积分", "统计学", "市场营销学", "消费者行为学", "市场调研", "广告学", "电子商务", "品牌管理", "销售管理", "毕业设计"));
        templates.put("工商管理", Arrays.asList("微积分", "统计学", "管理学原理", "市场营销", "人力资源管理", "财务管理", "运营管理", "战略管理", "组织行为学", "毕业设计"));
        templates.put("经济学", Arrays.asList("微积分", "线性代数", "政治经济学", "微观经济学", "宏观经济学", "计量经济学", "金融学", "财政学", "国际经济学", "毕业设计"));
        templates.put("金融学", Arrays.asList("微积分", "线性代数", "微观经济学", "宏观经济学", "金融学", "金融市场学", "商业银行管理", "投资学", "国际金融", "毕业设计"));
        templates.put("法学", Arrays.asList("法理学", "宪法学", "民法学", "刑法学", "行政法学", "经济法学", "诉讼法学", "国际法学", "商法学", "毕业设计"));
        templates.put("汉语言文学", Arrays.asList("现代汉语", "古代汉语", "中国古代文学", "中国现当代文学", "外国文学", "文学理论", "语言学概论", "写作", "比较文学", "毕业设计"));
        templates.put("英语", Arrays.asList("基础英语", "高级英语", "英语听力", "英语口语", "英语阅读", "英语写作", "翻译理论与实践", "英美文学", "语言学导论", "毕业设计"));
        templates.put("机械工程", Arrays.asList("高等数学", "工程力学", "机械制图", "机械设计", "机械原理", "材料力学", "电工电子技术", "机械制造技术", "自动控制原理", "毕业设计"));
        templates.put("土木工程", Arrays.asList("高等数学", "工程力学", "土木工程制图", "材料力学", "结构力学", "土力学", "混凝土结构", "钢结构", "土木工程施工", "毕业设计"));
        templates.put("电子信息工程", Arrays.asList("高等数学", "线性代数", "电路分析", "模拟电子技术", "数字电子技术", "信号与系统", "通信原理", "嵌入式系统", "数字信号处理", "毕业设计"));
        templates.put("通信工程", Arrays.asList("高等数学", "线性代数", "电路分析", "模拟电子技术", "数字电子技术", "信号与系统", "通信原理", "移动通信", "光纤通信", "毕业设计"));
        templates.put("自动化", Arrays.asList("高等数学", "线性代数", "电路分析", "自动控制原理", "传感器技术", "电机与拖动", "PLC原理", "过程控制", "运动控制", "毕业设计"));

        List<String> courses = templates.get(majorName);
        if (courses != null) return courses;
        // 默认课程模板
        return Arrays.asList("专业导论", "学科基础", "专业核心1", "专业核心2", "专业核心3", "专业选修1", "专业选修2", "实践课程", "毕业设计");
    }
}
