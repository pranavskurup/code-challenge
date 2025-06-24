package com.device.manager.application.service.mapper

import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.domain.entity.Device
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.UUID

object DeviceMapper {

    private val logger = LoggerFactory.getLogger(DeviceMapper::class.java)

    fun toEntity(request: CreateDeviceRequest): Device {
        logger.debug("Mapping CreateDeviceRequest to Device entity: {} - {}", request.name, request.brand)
        return Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = ZonedDateTime.now(),
            updatedTime = ZonedDateTime.now()
        ).also {
            logger.debug("Created Device entity with ID: {}", it.id)
        }
    }

    fun toResponse(device: Device): DeviceResponse {
        logger.debug("Mapping Device entity to DeviceResponse: {} ({})", device.name, device.id)
        return DeviceResponse(
            id = device.id!!,
            name = device.name,
            type = device.type,
            brand = device.brand,
            state = device.state,
            creationTime = device.creationTime.toString(),
            updatedTime = device.updatedTime.toString()
        )
    }

    fun toResponseList(devices: List<Device>): List<DeviceResponse> {
        logger.debug("Mapping {} Device entities to DeviceResponse list", devices.size)
        return devices.map { toResponse(it) }.also {
            logger.debug("Mapped {} devices to response list", it.size)
        }
    }
}
