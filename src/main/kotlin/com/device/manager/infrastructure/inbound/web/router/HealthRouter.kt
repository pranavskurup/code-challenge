package com.device.manager.infrastructure.inbound.web.router

import com.device.manager.infrastructure.inbound.web.handler.HealthHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
@Tag(name = "Health Check", description = "Health monitoring and application information endpoints")
class HealthRouter(
    private val healthHandler: HealthHandler
) {

    private val logger = LoggerFactory.getLogger(HealthRouter::class.java)

    init {
        logger.info("HealthRouter initialized - health and info endpoints configured")
    }

    @Bean
    @RouterOperations(
        RouterOperation(
            path = "/api/v1/health",
            method = [RequestMethod.GET],
            beanClass = HealthHandler::class,
            beanMethod = "health"
        ),
        RouterOperation(
            path = "/api/v1/info",
            method = [RequestMethod.GET],
            beanClass = HealthHandler::class,
            beanMethod = "info"
        )
    )
    fun healthRoutes(): RouterFunction<ServerResponse> = coRouter {
        "/api/v1".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("/health", healthHandler::health)
                GET("/info", healthHandler::info)
            }
        }
    }
}
