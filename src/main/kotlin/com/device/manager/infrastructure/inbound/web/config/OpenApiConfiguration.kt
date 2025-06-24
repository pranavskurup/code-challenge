package com.device.manager.infrastructure.inbound.web.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Device Manager API")
                    .description("RESTful API for managing devices with reactive Spring WebFlux")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Device Manager Team")
                            .email("support@device-manager.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Development server")
            )
            .addServersItem(
                Server()
                    .url("https://api.device-manager.com")
                    .description("Production server")
            )
    }
}
