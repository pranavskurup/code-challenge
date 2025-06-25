package com.device.manager.infrastructure.inbound.web.handler

import com.device.manager.infrastructure.inbound.web.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.time.ZonedDateTime

@Component
class HealthHandler(
    private val healthEndpoint: HealthEndpoint
) {

    private val logger = LoggerFactory.getLogger(HealthHandler::class.java)

    suspend fun health(request: ServerRequest): ServerResponse {
        logger.debug("Health check requested")

        val health = healthEndpoint.health()
        logger.debug("Health status: {}", health.status.code)

        val healthData = mapOf(
            "status" to health.status.code,
            "timestamp" to ZonedDateTime.now().toString()
        )

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ApiResponse.success(healthData))
    }

    suspend fun info(request: ServerRequest): ServerResponse {
        logger.debug("Application info requested")

        val info = mapOf(
            "application" to "Device Manager",
            "version" to "1.0.0",
            "description" to "Device management service with reactive Spring WebFlux",
            "features" to listOf(
                "Device CRUD operations",
                "Device state management",
                "Brand and state filtering",
                "Reactive programming with WebFlux",
                "Functional router/handler pattern"
            )
        )

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ApiResponse.success(info))
    }
}
