package com.device.manager.infrastructure.inbound.web.handler

import com.device.manager.infrastructure.inbound.web.dto.ApiResponse
import com.device.manager.infrastructure.inbound.web.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.time.ZonedDateTime

@Component
@Tag(name = "Health Check", description = "Health monitoring and application information endpoints")
class HealthHandler(
    private val healthEndpoint: HealthEndpoint
) {

    private val logger = LoggerFactory.getLogger(HealthHandler::class.java)

    @Operation(
        summary = "Get application health status",
        description = """
            Returns the current health status of the application including all health indicators.
            
            **Health Status Values:**
            - UP: Application is healthy and all dependencies are working
            - DOWN: Application or one of its dependencies is not working
            
            **Use Cases:**
            - Load balancer health checks
            - Monitoring and alerting systems
            - Service discovery health validation
        """,
        tags = ["Health Check"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Health status retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Healthy Application",
                            summary = "Application is healthy and all systems are operational",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": {
                                    "status": "UP",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                },
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        ),
                        ExampleObject(
                            name = "Unhealthy Application",
                            summary = "Application or dependencies are not healthy",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": {
                                    "status": "DOWN",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                },
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        )
                    ]
                )]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Health Check Error",
                        value = """
                        {
                            "code": "HEALTH_CHECK_ERROR",
                            "message": "Failed to retrieve health status",
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            )
        ]
    )
    suspend fun health(request: ServerRequest): ServerResponse {
        logger.debug("Health check requested")

        return withContext(Dispatchers.IO) {
            val health = healthEndpoint.health()
            logger.debug("Health status: {}", health.status.code)

            val healthData = mapOf(
                "status" to health.status.code,
                "timestamp" to ZonedDateTime.now().toString()
            )

            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ApiResponse.success(healthData))
        }
    }

    @Operation(
        summary = "Get application information",
        description = """
            Returns detailed information about the application including version, description, and features.
            
            **Information Included:**
            - Application name and version
            - Service description
            - Available features and capabilities
            - Technology stack information
            
            **Use Cases:**
            - Service discovery and documentation
            - Client application configuration
            - Development and debugging
            - API documentation generation
        """,
        tags = ["Health Check"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Application information retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Application Info",
                        summary = "Complete application information and features",
                        value = """
                        {
                            "success": true,
                            "message": null,
                            "data": {
                                "application": "Device Manager",
                                "version": "1.0.0",
                                "description": "Device management service with reactive Spring WebFlux",
                                "features": [
                                    "Device CRUD operations",
                                    "Device state management",
                                    "Brand and state filtering",
                                    "Reactive programming with WebFlux",
                                    "Functional router/handler pattern"
                                ]
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Info Retrieval Error",
                        value = """
                        {
                            "code": "INFO_RETRIEVAL_ERROR",
                            "message": "Failed to retrieve application information",
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            )
        ]
    )
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
