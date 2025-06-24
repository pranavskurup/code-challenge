package com.device.manager.application.service.dto

import com.device.manager.domain.entity.DeviceState
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(
    description = "Request to create a new device",
    example = """
    {
        "name": "iPhone 15 Pro",
        "type": "smartphone",
        "brand": "Apple",
        "state": "inactive"
    }
    """
)
data class CreateDeviceRequest(
    @Schema(
        description = "Device name - must be unique within the same brand",
        example = "iPhone 15 Pro",
        required = true,
        minLength = 1,
        maxLength = 255
    )
    val name: String,
    
    @Schema(
        description = "Type/category of the device",
        example = "smartphone",
        required = true,
        minLength = 1,
        maxLength = 100
    )
    val type: String,
    
    @Schema(
        description = "Brand or manufacturer of the device",
        example = "Apple",
        required = true,
        minLength = 1,
        maxLength = 100
    )
    val brand: String,
    
    @Schema(
        description = "Initial state of the device. Defaults to INACTIVE if not specified",
        example = "inactive",
        required = false,
        allowableValues = ["available", "in-use", "inactive"],
        defaultValue = "inactive"
    )
    val state: DeviceState = DeviceState.INACTIVE
)

@Schema(
    description = "Request to update an existing device. All fields are optional - only provided fields will be updated",
    example = """
    {
        "name": "iPhone 15 Pro Max",
        "state": "available"
    }
    """
)
data class UpdateDeviceRequest(
    @Schema(
        description = "New device name. Cannot be changed for devices in IN_USE state",
        example = "iPhone 15 Pro Max",
        required = false,
        minLength = 1,
        maxLength = 255
    )
    val name: String?,
    
    @Schema(
        description = "New device type/category",
        example = "smartphone",
        required = false,
        minLength = 1,
        maxLength = 100
    )
    val type: String?,
    
    @Schema(
        description = "New device brand. Cannot be changed for devices in IN_USE state",
        example = "Apple",
        required = false,
        minLength = 1,
        maxLength = 100
    )
    val brand: String?,
    
    @Schema(
        description = "New device state. State transitions follow business rules",
        example = "available",
        required = false,
        allowableValues = ["available", "in-use", "inactive"]
    )
    val state: DeviceState?
)

@Schema(
    description = "Device information response",
    example = """
    {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "iPhone 15 Pro",
        "type": "smartphone",
        "brand": "Apple",
        "state": "available",
        "creationTime": "2024-01-15T10:30:00Z",
        "updatedTime": "2024-01-15T14:45:00Z"
    }
    """
)
data class DeviceResponse(
    @Schema(
        description = "Unique identifier of the device",
        example = "550e8400-e29b-41d4-a716-446655440000",
        required = true
    )
    val id: UUID,
    
    @Schema(
        description = "Name of the device",
        example = "iPhone 15 Pro",
        required = true
    )
    val name: String,
    
    @Schema(
        description = "Type/category of the device",
        example = "smartphone",
        required = true
    )
    val type: String,
    
    @Schema(
        description = "Brand or manufacturer of the device",
        example = "Apple",
        required = true
    )
    val brand: String,
    
    @Schema(
        description = "Current state of the device",
        example = "available",
        required = true,
        allowableValues = ["available", "in-use", "inactive"]
    )
    val state: DeviceState,
    
    @Schema(
        description = "Timestamp when the device was created (ISO 8601 format)",
        example = "2024-01-15T10:30:00Z",
        required = true
    )
    val creationTime: String,
    
    @Schema(
        description = "Timestamp when the device was last updated (ISO 8601 format)",
        example = "2024-01-15T14:45:00Z",
        required = true
    )
    val updatedTime: String
)
