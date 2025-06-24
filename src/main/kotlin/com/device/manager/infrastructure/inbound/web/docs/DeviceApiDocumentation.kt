package com.device.manager.infrastructure.inbound.web.docs

import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.infrastructure.inbound.web.dto.ApiResponse
import com.device.manager.infrastructure.inbound.web.dto.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * OpenAPI documentation for Device Management endpoints.
 * This interface provides comprehensive API documentation for all device operations.
 */
@Tag(name = "Device Management", description = "Device Management and Query Operations")
interface DeviceApiDocumentation {

    @Operation(
        summary = "Create a new device",
        description = """
            Creates a new device in the system with the provided information.
            
            **Business Rules:**
            - Device name must be unique within the same brand
            - Device starts in INACTIVE state by default
            - All fields except state are required
            
            **State Information:**
            - **INACTIVE**: Default state for new devices
            - **AVAILABLE**: Device is ready to be used  
            - **IN_USE**: Device is currently being used
        """,
        tags = ["Device Management"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "201",
                description = "Device created successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Success Response",
                                description = "Successful device creation",
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
                                    },
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid request - validation failed",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Duplicate Device Error",
                                description = "Device name already exists within the brand",
                                value = """
                                {
                                    "code": "DUPLICATE_DEVICE",
                                    "message": "Device with name 'iPhone 15 Pro' already exists for brand 'Apple'",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            ),
                            ExampleObject(
                                name = "Validation Error",
                                description = "Invalid request body",
                                value = """
                                {
                                    "code": "INVALID_REQUEST_BODY",
                                    "message": "Invalid JSON format",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Internal Error",
                                value = """
                                {
                                    "code": "INTERNAL_ERROR",
                                    "message": "An unexpected error occurred",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    @RequestBody(
        description = "Device information to create",
        required = true,
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = CreateDeviceRequest::class),
                examples = [
                    ExampleObject(
                        name = "Create iPhone",
                        description = "Creating an iPhone device",
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
                        description = "Creating a laptop device",
                        value = """
                        {
                            "name": "MacBook Pro M3",
                            "type": "laptop",
                            "brand": "Apple",
                            "state": "available"
                        }
                        """
                    )
                ]
            )
        ]
    )
    fun createDevice()

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
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Device List",
                                description = "Successful retrieval of device list",
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
                                description = "No devices found",
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
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    fun getAllDevices()

    @Operation(
        summary = "Get device by ID",
        description = """
            Retrieves a specific device by its unique identifier.
            
            Returns complete device information including current state and timestamps.
        """,
        tags = ["Device Management"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Device retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
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
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid device ID format",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
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
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Device Not Found",
                                value = """
                                {
                                    "code": "DEVICE_NOT_FOUND",
                                    "message": "Device with ID 550e8400-e29b-41d4-a716-446655440000 not found",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Parameter(
        name = "id",
        description = "Unique identifier of the device (UUID format)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "550e8400-e29b-41d4-a716-446655440000",
        schema = Schema(type = "string", format = "uuid")
    )
    fun getDevice()

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
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Device updated successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
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
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid request or business rule violation",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "In-Use Device Immutable Fields",
                                description = "Attempted to update name/brand of in-use device",
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
                                description = "Invalid device state transition",
                                value = """
                                {
                                    "code": "DEVICE_STATE_CHANGE_ERROR",
                                    "message": "Invalid state transition from in-use to available",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json", 
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
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
        content = [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = UpdateDeviceRequest::class),
                examples = [
                    ExampleObject(
                        name = "Update Name Only",
                        description = "Update only the device name",
                        value = """
                        {
                            "name": "iPhone 15 Pro Max"
                        }
                        """
                    ),
                    ExampleObject(
                        name = "Update State Only",
                        description = "Change device state",
                        value = """
                        {
                            "state": "available"
                        }
                        """
                    ),
                    ExampleObject(
                        name = "Update Multiple Fields",
                        description = "Update multiple fields at once",
                        value = """
                        {
                            "name": "iPhone 15 Pro Max",
                            "type": "flagship-smartphone",
                            "state": "available"
                        }
                        """
                    )
                ]
            )
        ]
    )
    fun updateDevice()

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
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "204",
                description = "Device deleted successfully"
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Cannot delete device - business rule violation",
                content = [
                    Content(
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
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "404",
                description = "Device not found",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Parameter(
        name = "id",
        description = "Unique identifier of the device to delete (UUID format)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "550e8400-e29b-41d4-a716-446655440000",
        schema = Schema(type = "string", format = "uuid")
    )
    fun deleteDevice()

    @Operation(
        summary = "Get devices by brand",
        description = """
            Retrieves all devices from a specific brand.
            
            This endpoint allows filtering devices by their brand/manufacturer.
            The brand parameter is case-sensitive.
        """,
        tags = ["Device Queries"]
    )
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Devices retrieved successfully",
                content = [
                    Content(
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
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
    )
    @Parameter(
        name = "brand",
        description = "Brand name to filter devices (case-sensitive)",
        required = true,
        `in` = ParameterIn.PATH,
        example = "Apple",
        schema = Schema(type = "string")
    )
    fun getDevicesByBrand()

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
    @ApiResponses(
        value = [
            OpenApiResponse(
                responseCode = "200",
                description = "Devices retrieved successfully",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
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
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "400",
                description = "Invalid device state",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Invalid State",
                                value = """
                                {
                                    "code": "INVALID_STATE",
                                    "message": "Invalid device state. Valid states: [available, in-use, inactive]",
                                    "timestamp": "2024-01-15T10:30:00Z"
                                }
                                """
                            )
                        ]
                    )
                ]
            ),
            OpenApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class)
                    )
                ]
            )
        ]
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
    fun getDevicesByState()
}
