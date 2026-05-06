package com.schemaplexai.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemaPlexAI API")
                        .description("SchemaPlexAI AI R&D Collaboration Platform API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SchemaPlexAI Team")
                                .email("support@schemaplexai.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your Bearer JWT token obtained from /auth/login"));
    }

    /** Web layer APIs (port 8082) */
    @Bean
    public GroupedOpenApi webApi() {
        return GroupedOpenApi.builder()
                .group("01-web")
                .displayName("Web (8082)")
                .pathsToMatch("/web/**", "/sse/**", "/ws/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** System/Auth APIs (port 8081) */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("02-system")
                .displayName("System & Auth (8081)")
                .pathsToMatch("/system/**", "/auth/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Agent Configuration APIs (port 8083) */
    @Bean
    public GroupedOpenApi agentConfigApi() {
        return GroupedOpenApi.builder()
                .group("03-agent-config")
                .displayName("Agent Config (8083)")
                .pathsToMatch("/agent-config/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Agent Engine APIs (port 8084) */
    @Bean
    public GroupedOpenApi agentEngineApi() {
        return GroupedOpenApi.builder()
                .group("04-agent-engine")
                .displayName("Agent Engine (8084)")
                .pathsToMatch("/agent/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Context/RAG APIs (port 8085) */
    @Bean
    public GroupedOpenApi contextApi() {
        return GroupedOpenApi.builder()
                .group("05-context")
                .displayName("Context & RAG (8085)")
                .pathsToMatch("/context/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Spec APIs (port 8086) */
    @Bean
    public GroupedOpenApi specApi() {
        return GroupedOpenApi.builder()
                .group("06-spec")
                .displayName("Spec (8086)")
                .pathsToMatch("/spec/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Workflow APIs (port 8087) */
    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("07-workflow")
                .displayName("Workflow (8087)")
                .pathsToMatch("/workflow/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Integration APIs (port 8088) */
    @Bean
    public GroupedOpenApi integrationApi() {
        return GroupedOpenApi.builder()
                .group("08-integration")
                .displayName("Integration (8088)")
                .pathsToMatch("/integration/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Ops APIs (port 8089) */
    @Bean
    public GroupedOpenApi opsApi() {
        return GroupedOpenApi.builder()
                .group("09-ops")
                .displayName("Ops (8089)")
                .pathsToMatch("/ops/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    /** Quality APIs (port 8090) */
    @Bean
    public GroupedOpenApi qualityApi() {
        return GroupedOpenApi.builder()
                .group("10-quality")
                .displayName("Quality (8090)")
                .pathsToMatch("/quality/**")
                .addOpenApiCustomizer(openApi -> openApi.getComponents()
                        .addParameters(TENANT_HEADER, tenantIdParameter()))
                .build();
    }

    private Parameter tenantIdParameter() {
        return new Parameter()
                .name(TENANT_HEADER)
                .in("header")
                .required(false)
                .description("Tenant identifier for multi-tenant data isolation")
                .schema(new io.swagger.v3.oas.models.media.StringSchema());
    }
}
