package com.device.manager.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Device Manager API")
                    .description("Comprehensive API for managing electronic devices with support for CRUD operations and advanced querying capabilities")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Device Manager Team")
                            .email("support@devicemanager.com")
                            .url("https://devicemanager.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Development Server"),
                    Server()
                        .url("https://api.devicemanager.com")
                        .description("Production Server")
                )
            )
    }

    @Bean
    fun deviceApiGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("device-api")
            .displayName("Device Management API")
            .pathsToMatch("/api/v1/devices/**")
            .build()
    }
}
