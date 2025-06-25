package com.device.manager.infrastructure.inbound.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

@Schema(
    description = "Standard API response wrapper for all endpoints",
    example = """
    {
        "success": true,
        "message": "Device created successfully",
        "data": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "iPhone 15 Pro",
            "type": "smartphone",
            "brand": "Apple",
            "state": "available"
        },
        "timestamp": "2024-01-15T10:30:00Z"
    }
    """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    @Schema(
        description = "Indicates whether the operation was successful",
        example = "true",
        required = true
    )
    val success: Boolean,
    
    @Schema(
        description = "Human-readable message describing the result",
        example = "Device created successfully",
        required = false
    )
    val message: String? = null,
    
    @Schema(
        description = "The actual response data (if successful)",
        required = false
    )
    val data: T? = null,
    
    @Schema(
        description = "Timestamp of when the response was generated (ISO 8601 format)",
        example = "2024-01-15T10:30:00Z",
        required = true
    )
    val timestamp: String = ZonedDateTime.now().toString()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                message = message,
                data = data
            )
        }

        fun <T> error(message: String, data: T? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                message = message,
                data = data
            )
        }
    }
}

@Schema(
    description = "Error response structure for failed operations",
    example = """
    {
        "code": "DEVICE_NOT_FOUND",
        "message": "Device with ID 550e8400-e29b-41d4-a716-446655440000 not found",
        "timestamp": "2024-01-15T10:30:00Z"
    }
    """
)
data class ErrorResponse(
    @Schema(
        description = "Error code for programmatic handling",
        example = "DEVICE_NOT_FOUND",
        required = true
    )
    val code: String,
    
    @Schema(
        description = "Human-readable error message",
        example = "Device with ID 550e8400-e29b-41d4-a716-446655440000 not found",
        required = true
    )
    val message: String,
    
    @Schema(
        description = "Timestamp when the error occurred (ISO 8601 format)",
        example = "2024-01-15T10:30:00Z",
        required = true
    )
    val timestamp: String = ZonedDateTime.now().toString()
)
