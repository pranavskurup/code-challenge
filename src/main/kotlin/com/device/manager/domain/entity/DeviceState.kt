package com.device.manager.domain.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Device state enumeration representing the current status of a device",
    example = "available",
    allowableValues = ["available", "in-use", "inactive"]
)
enum class DeviceState(
    @Schema(
        description = "String representation of the device state",
        example = "available"
    )
    val state: String
) {
    @Schema(description = "Device is ready to be used")
    AVAILABLE("available"),

    @Schema(description = "Device is currently being used")
    IN_USE("in-use"),

    @Schema(description = "Device is not available for use (default state)")
    INACTIVE("inactive");

    @JsonValue
    fun toValue(): String = state

    companion object {
        @JsonCreator
        @JvmStatic
        fun of(state: String): DeviceState {
            return entries.firstOrNull { it.state == state } ?: entries.firstOrNull { it.name == state }?: AVAILABLE
        }
    }

}
