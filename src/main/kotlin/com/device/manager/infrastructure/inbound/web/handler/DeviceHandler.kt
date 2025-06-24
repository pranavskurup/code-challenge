package com.device.manager.infrastructure.inbound.web.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.DeviceService
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.infrastructure.inbound.web.dto.ApiResponse
import com.device.manager.infrastructure.inbound.web.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono
import java.util.UUID

@Component
@Tag(name = "Device Management", description = "Device Management and Query Operations")
class DeviceHandler(
    private val deviceService: DeviceService
) {
    
    private val logger = LoggerFactory.getLogger(DeviceHandler::class.java)

    @Operation(
        summary = "Create a new device",
        description = """
            Creates a new device in the system with the provided information.
            
            **Business Rules:**
            - Device name must be unique within the same brand
            - Device starts in INACTIVE state by default
            - All fields except state are required
        """,
        tags = ["Device Management"]
    )
    @RequestBody(
        description = "Device information to create",
        required = true,
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateDeviceRequest::class),
            examples = [
                ExampleObject(
                    name = "Create Smartphone",
                    summary = "Create an iPhone device",
                    value = """
                    {
                        "name": "iPhone 15 Pro",
                        "type": "smartphone",
                        "brand": "Apple",
                        "state": "inactive"
                    }
                    """
                ),
                ExampleObject(
                    name = "Create Laptop",
                    summary = "Create a MacBook device",
                    value = """
                    {
                        "name": "MacBook Pro M3",
                        "type": "laptop",
                        "brand": "Apple",
                        "state": "available"
                    }
                    """
                ),
                ExampleObject(
                    name = "Create Tablet",
                    summary = "Create an iPad device",
                    value = """
                    {
                        "name": "iPad Pro 12.9",
                        "type": "tablet",
                        "brand": "Apple"
                    }
                    """
                )
            ]
        )]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "201",
                description = "Device created successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """
                        {
                            "success": true,
                            "message": "Device created successfully",
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "iPhone 15 Pro",
                                "type": "smartphone",
                                "brand": "Apple",
                                "state": "inactive",
                                "creationTime": "2024-01-15T10:30:00Z",
                                "updatedTime": "2024-01-15T10:30:00Z"
                            }
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun createDevice(request: ServerRequest): ServerResponse {
        logger.info("Received request to create new device")
        
        return try {
            val createRequest = request.awaitBodyOrNull<CreateDeviceRequest>()
                ?: run {
                    logger.warn("Invalid or missing request body for device creation")
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid or missing request body"))
                }

            logger.debug("Creating device with request: {}", createRequest)

            deviceService.execute(createRequest).fold(
                { error -> 
                    logger.warn("Failed to create device: {} - {}", error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device -> 
                    logger.info("Device created successfully with ID: {}", device.id)
                    ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device, "Device created successfully"))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while creating device", e)
            when {
                e.message?.contains("Failed to read HTTP message") == true ||
                e.message?.contains("JSON") == true ||
                e.message?.contains("parse") == true -> {
                    logger.warn("Invalid JSON format in request body")
                    ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid JSON format"))
                }
                else -> buildInternalErrorResponse(e)
            }
        }
    }

    /**
     * Get device by ID
     * GET /api/v1/devices/{id}
     */
    @Operation(
        summary = "Get device by ID",
        description = """
            Retrieves a specific device by its unique identifier.
            
            Returns complete device information including current state and timestamps.
        """,
        tags = ["Device Management"]
    )
    @Parameter(
        name = "id",
        description = "Unique identifier of the device (UUID format)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "550e8400-e29b-41d4-a716-446655440000",
        schema = Schema(type = "string", format = "uuid")
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Device retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Device Found",
                        value = """
                        {
                            "success": true,
                            "message": null,
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "iPhone 15 Pro",
                                "type": "smartphone",
                                "brand": "Apple",
                                "state": "available",
                                "creationTime": "2024-01-15T10:30:00Z",
                                "updatedTime": "2024-01-15T14:45:00Z"
                            },
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid device ID format",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid UUID",
                        value = """
                        {
                            "code": "INVALID_UUID",
                            "message": "Invalid device ID format",
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Device Not Found",
                        value = """
                        {
                            "code": "DEVICE_NOT_FOUND",
                            "message": "Device with ID 550e8400-e29b-41d4-a716-446655440000 not found",
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
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun getDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to get device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)
            logger.debug("Parsed device ID: {}", id)

            deviceService.execute(id).fold(
                { error -> 
                    logger.warn("Failed to get device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device ->
                    logger.debug("Successfully retrieved device: {}", device.id)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device))
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while getting device with ID: {}", deviceId, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Update device
     * PUT /api/v1/devices/{id}
     */
    @Operation(
        summary = "Update device by ID", 
        description = """
            Updates an existing device with the provided information. Only provided fields will be updated.
            
            **Business Rules:**
            - Name and brand cannot be updated for devices in IN_USE state
            - Device state transitions must follow business rules:
              - INACTIVE → AVAILABLE (allowed)
              - AVAILABLE → IN_USE (allowed) 
              - IN_USE → INACTIVE (allowed)
              - All other transitions are not allowed
            - Device name must remain unique within the same brand
            
            **Partial Updates:**
            All fields in the request body are optional. Only fields provided will be updated.
        """,
        tags = ["Device Management"]
    )
    @Parameter(
        name = "id",
        description = "Unique identifier of the device to update (UUID format)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "550e8400-e29b-41d4-a716-446655440000",
        schema = Schema(type = "string", format = "uuid")
    )
    @RequestBody(
        description = "Device fields to update (all fields are optional)",
        required = true,
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = UpdateDeviceRequest::class),
            examples = [
                ExampleObject(
                    name = "Update Name Only",
                    summary = "Update only the device name",
                    value = """
                    {
                        "name": "iPhone 15 Pro Max"
                    }
                    """
                ),
                ExampleObject(
                    name = "Update State Only",
                    summary = "Change device state",
                    value = """
                    {
                        "state": "available"
                    }
                    """
                ),
                ExampleObject(
                    name = "Update Multiple Fields",
                    summary = "Update multiple fields at once",
                    value = """
                    {
                        "name": "iPhone 15 Pro Max",
                        "type": "flagship-smartphone",
                        "state": "available"
                    }
                    """
                )
            ]
        )]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Device updated successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Update Success",
                        value = """
                        {
                            "success": true,
                            "message": "Device updated successfully",
                            "data": {
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": "iPhone 15 Pro Max",
                                "type": "smartphone",
                                "brand": "Apple",
                                "state": "available",
                                "creationTime": "2024-01-15T10:30:00Z",
                                "updatedTime": "2024-01-15T16:20:00Z"
                            },
                            "timestamp": "2024-01-15T16:20:00Z"
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid request or business rule violation",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "In-Use Device Immutable Fields",
                            summary = "Attempted to update name/brand of in-use device",
                            value = """
                            {
                                "code": "IN_USE_DEVICE_IMMUTABLE_FIELDS",
                                "message": "Cannot update name or brand of device in use",
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        ),
                        ExampleObject(
                            name = "Invalid State Transition", 
                            summary = "Invalid device state transition",
                            value = """
                            {
                                "code": "DEVICE_STATE_CHANGE_ERROR",
                                "message": "Invalid state transition from in-use to available",
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        )
                    ]
                )]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json", 
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun updateDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to update device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)
            val updateRequest = request.awaitBodyOrNull<UpdateDeviceRequest>()
                ?: run {
                    logger.warn("Invalid or missing request body for device update")
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid or missing request body"))
                }

            logger.debug("Updating device {} with request: {}", id, updateRequest)

            deviceService.execute(id, updateRequest).fold(
                { error -> 
                    logger.warn("Failed to update device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device ->
                    logger.info("Device updated successfully: {}", device.id)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device, "Device updated successfully"))
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided for update: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while updating device with ID: {}", deviceId, e)
            when {
                e.message?.contains("Failed to read HTTP message") == true ||
                e.message?.contains("JSON") == true ||
                e.message?.contains("parse") == true -> {
                    logger.warn("Invalid JSON format in update request body")
                    ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid JSON format"))
                }
                else -> buildInternalErrorResponse(e)
            }
        }
    }

    /**
     * Delete device
     * DELETE /api/v1/devices/{id}
     */
    @Operation(
        summary = "Delete device by ID",
        description = """
            Permanently deletes a device from the system.
            
            **Business Rules:**
            - Devices in IN_USE state cannot be deleted
            - Device must be in AVAILABLE or INACTIVE state to be deleted
            - This operation is irreversible
        """,
        tags = ["Device Management"]
    )
    @Parameter(
        name = "id",
        description = "Unique identifier of the device to delete (UUID format)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "550e8400-e29b-41d4-a716-446655440000",
        schema = Schema(type = "string", format = "uuid")
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "204",
                description = "Device deleted successfully"
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Cannot delete device - business rule violation",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "In-Use Device Cannot Be Deleted",
                            value = """
                            {
                                "code": "IN_USE_DEVICE_CANNOT_BE_DELETED",
                                "message": "Cannot delete device that is currently in use",
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        ),
                        ExampleObject(
                            name = "Invalid UUID",
                            value = """
                            {
                                "code": "INVALID_UUID",
                                "message": "Invalid device ID format",
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        )
                    ]
                )]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun deleteDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to delete device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)

            deviceService.deleteDevice(id).fold(
                { error -> 
                    logger.warn("Failed to delete device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                {
                    logger.info("Device deleted successfully: {}", id)
                    ServerResponse.noContent().buildAndAwait()
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided for deletion: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while deleting device with ID: {}", deviceId, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get all devices
     * GET /api/v1/devices
     */
    @Operation(
        summary = "Get all devices",
        description = """
            Retrieves a list of all devices in the system.
            
            Returns devices with their current state and metadata including creation and update timestamps.
        """,
        tags = ["Device Management"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "List of devices retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Multiple Devices",
                            summary = "List with multiple devices",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": [
                                    {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "name": "iPhone 15 Pro",
                                        "type": "smartphone",
                                        "brand": "Apple",
                                        "state": "available",
                                        "creationTime": "2024-01-15T10:30:00Z",
                                        "updatedTime": "2024-01-15T14:45:00Z"
                                    },
                                    {
                                        "id": "550e8400-e29b-41d4-a716-446655440001",
                                        "name": "Galaxy S24",
                                        "type": "smartphone",
                                        "brand": "Samsung",
                                        "state": "in-use",
                                        "creationTime": "2024-01-14T09:15:00Z",
                                        "updatedTime": "2024-01-15T11:20:00Z"
                                    }
                                ],
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        ),
                        ExampleObject(
                            name = "Empty List",
                            summary = "No devices found",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": [],
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
                        name = "Internal Error",
                        value = """
                        {
                            "code": "INTERNAL_ERROR",
                            "message": "An unexpected error occurred",
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            )
        ]
    )
    suspend fun getAllDevices(request: ServerRequest): ServerResponse {
        logger.info("Received request to get all devices")
        
        return try {
            deviceService.execute().fold(
                { error -> 
                    logger.warn("Failed to get all devices: {} - {}", error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices", devices.size)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting all devices", e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get devices by brand
     * GET /api/v1/devices/brand/{brand}
     */
    @Operation(
        summary = "Get devices by brand",
        description = """
            Retrieves all devices from a specific brand.
            
            This endpoint allows filtering devices by their brand/manufacturer.
            The brand parameter is case-sensitive.
        """,
        tags = ["Device Queries"]
    )
    @Parameter(
        name = "brand",
        description = "Brand name to filter devices (case-sensitive)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "Apple",
        schema = Schema(type = "string")
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Devices retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Apple Devices",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": [
                                    {
                                        "id": "550e8400-e29b-41d4-a716-446655440000",
                                        "name": "iPhone 15 Pro",
                                        "type": "smartphone",
                                        "brand": "Apple",
                                        "state": "available",
                                        "creationTime": "2024-01-15T10:30:00Z",
                                        "updatedTime": "2024-01-15T14:45:00Z"
                                    },
                                    {
                                        "id": "550e8400-e29b-41d4-a716-446655440001",
                                        "name": "MacBook Pro M3",
                                        "type": "laptop",
                                        "brand": "Apple",
                                        "state": "inactive",
                                        "creationTime": "2024-01-14T09:15:00Z",
                                        "updatedTime": "2024-01-14T09:15:00Z"
                                    }
                                ],
                                "timestamp": "2024-01-15T10:30:00Z"
                            }
                            """
                        ),
                        ExampleObject(
                            name = "No Devices Found",
                            value = """
                            {
                                "success": true,
                                "message": null,
                                "data": [],
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
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun getDevicesByBrand(request: ServerRequest): ServerResponse {
        val brand = request.pathVariable("brand")
        logger.info("Received request to get devices by brand: {}", brand)
        
        return try {
            if (brand.isBlank()) {
                logger.warn("Empty brand parameter provided")
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValueAndAwait(ErrorResponse("INVALID_BRAND", "Brand cannot be empty"))
            }

            deviceService.execute(brand).fold(
                { error -> 
                    logger.warn("Failed to get devices by brand '{}': {} - {}", brand, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices for brand: {}", devices.size, brand)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting devices by brand: {}", brand, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get devices by state
     * GET /api/v1/devices/state/{state}
     */
    @Operation(
        summary = "Get devices by state",
        description = """
            Retrieves all devices with a specific state.
            
            This endpoint allows filtering devices by their current state.
            
            **Valid States:**
            - **available**: Devices ready to be used
            - **in-use**: Devices currently being used
            - **inactive**: Devices not available for use
            
            The state parameter is case-insensitive.
        """,
        tags = ["Device Queries"]
    )
    @Parameter(
        name = "state",
        description = "Device state to filter by (case-insensitive)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "available",
        schema = Schema(
            type = "string",
            allowableValues = ["available", "in-use", "inactive"]
        )
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Devices retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Available Devices",
                        value = """
                        {
                            "success": true,
                            "message": null,
                            "data": [
                                {
                                    "id": "550e8400-e29b-41d4-a716-446655440000",
                                    "name": "iPhone 15 Pro",
                                    "type": "smartphone",
                                    "brand": "Apple",
                                    "state": "available",
                                    "creationTime": "2024-01-15T10:30:00Z",
                                    "updatedTime": "2024-01-15T14:45:00Z"
                                }
                            ],
                            "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )]
                )]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid device state",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "Invalid State",
                        value = """
                        {
                            "code": "INVALID_STATE",
                            "message": "Invalid device state. Valid states: [available, in-use, inactive]",
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
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    suspend fun getDevicesByState(request: ServerRequest): ServerResponse {
        val stateParam = request.pathVariable("state")
        logger.info("Received request to get devices by state: {}", stateParam)
        
        return try {
            val state = DeviceState.entries.find { it.state.equals(stateParam, ignoreCase = true) }
                ?: run {
                    logger.warn("Invalid device state provided: {}", stateParam)
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_STATE", "Invalid device state. Valid states: ${DeviceState.entries.map { it.state }}"))
                }

            logger.debug("Parsed device state: {}", state)

            deviceService.execute(state).fold(
                { error -> 
                    logger.warn("Failed to get devices by state '{}': {} - {}", state, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices for state: {}", devices.size, state)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting devices by state: {}", stateParam, e)
            buildInternalErrorResponse(e)
        }
    }

    private suspend fun buildErrorResponse(error: DomainError.DeviceError): ServerResponse {
        logger.debug("Building error response for: {} - {}", error.code, error.message)
        
        val status = when (error.code) {
            DomainError.DeviceError.DEVICE_NOT_FOUND -> HttpStatus.NOT_FOUND
            DomainError.DeviceError.DEVICE_ALREADY_EXISTS -> HttpStatus.CONFLICT
            DomainError.DeviceError.DEVICE_UNAUTHORIZED -> HttpStatus.FORBIDDEN
            DomainError.DeviceError.DEVICE_STATE_CHANGE_ERROR,
            DomainError.DeviceError.CREATION_TIME_IMMUTABLE,
            DomainError.DeviceError.IN_USE_DEVICE_IMMUTABLE_FIELDS,
            DomainError.DeviceError.IN_USE_DEVICE_CANNOT_BE_DELETED,
            "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ErrorResponse(error.code ?: "UNKNOWN_ERROR", error.message ?: "An error occurred"))
    }

    private suspend fun buildInternalErrorResponse(e: Exception): ServerResponse {
        logger.error("Building internal server error response", e)
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred: ${e.message}"))
    }
}
