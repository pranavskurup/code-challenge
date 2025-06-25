package com.device.manager.infrastructure.inbound.web.router

import com.device.manager.infrastructure.inbound.web.handler.HealthHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class HealthRouter(
    private val healthHandler: HealthHandler
) {

    @Bean
    fun healthRoutes() = coRouter {
        "/api/v1".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("/health", healthHandler::health)
                GET("/info", healthHandler::info)
            }
        }
    }
}
