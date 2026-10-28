package com.mindskip.xzs.configuration.spring;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * SpringDoc OpenAPI 配置
 * 访问地址：
 *   - Swagger UI: http://localhost:8001/swagger-ui/index.html
 *   - OpenAPI JSON: http://localhost:8001/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能教师管理系统 API 文档")
                        .description("基于大模型的个性化资源与学习路径规划系统\n\n"
                                + "**功能模块：**\n"
                                + "- 学生画像（8维度AI对话式构建）\n"
                                + "- 个性化学习路径规划\n"
                                + "- 多智能体资源生成（文档/思维导图/练习/阅读/视频/代码）\n"
                                + "- 知识库RAG问答\n"
                                + "- AI教案生成与PPT导出\n"
                                + "- 考试与题库管理\n"
                                + "- Bilibili视频集成\n\n"
                                + "**认证方式：** Bearer Token（登录后获取）")
                        .version("3.9.0")
                        .contact(new Contact()
                                .name("知汇贝客团队")
                                .url("https://www.mindskip.net"))
                        .license(new License()
                                .name("AGPL-3.0")
                                .url("https://www.gnu.org/licenses/agpl-3.0.txt")))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8001").description("本地开发环境"),
                        new Server().url("http://localhost:8001").description("测试环境")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录后获取的 Token，在请求头中添加：Authorization: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    // ========== API 分组定义 ==========

    /** 管理端 API 分组 */
    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("管理端")
                .displayName("管理端 API")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /** 学生端 API 分组 */
    @Bean
    public GroupedOpenApi studentApiGroup() {
        return GroupedOpenApi.builder()
                .group("学生端")
                .displayName("学生端 API")
                .pathsToMatch("/api/student/**")
                .build();
    }

    /** 微信端 API 分组 */
    @Bean
    public GroupedOpenApi wxApiGroup() {
        return GroupedOpenApi.builder()
                .group("微信端")
                .displayName("微信小程序 API")
                .pathsToMatch("/api/wx/**")
                .build();
    }

    /** 通用 API 分组 */
    @Bean
    public GroupedOpenApi commonApiGroup() {
        return GroupedOpenApi.builder()
                .group("通用")
                .displayName("通用 API（登录/注册等）")
                .pathsToMatch("/api/user/**")
                .build();
    }
}
