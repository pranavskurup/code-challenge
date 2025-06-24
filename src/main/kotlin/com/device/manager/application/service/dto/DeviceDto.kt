package com.device.manager.application.service.dto

import com.device.manager.domain.entity.DeviceState
import java.util.UUID

data class CreateDeviceRequest(
    val name: String,
    val type: String,
    val brand: String,
    val state: DeviceState = DeviceState.INACTIVE
)

data class UpdateDeviceRequest(
    val name: String?,
    val type: String?,
    val brand: String?,
    val state: DeviceState?
)

data class DeviceResponse(
    val id: UUID,
    val name: String,
    val type: String,
    val brand: String,
    val state: DeviceState,
    val creationTime: String,
    val updatedTime: String
)
