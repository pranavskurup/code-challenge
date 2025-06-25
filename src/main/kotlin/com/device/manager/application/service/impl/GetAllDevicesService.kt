package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.GetAllDevicesUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetAllDevicesService(
    private val deviceRepository: DeviceRepository
) : GetAllDevicesUseCase {

    private val logger = LoggerFactory.getLogger(GetAllDevicesService::class.java)

    override suspend fun execute(): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.info("Executing get all devices service")

        return try {
            logger.debug("Fetching all devices from repository")
            deviceRepository.findAll()
                .map { devices ->
                    logger.info("Successfully retrieved {} devices", devices.size)
                    logger.debug("Device count by brand: {}", devices.groupBy { it.brand }.mapValues { it.value.size })
                    DeviceMapper.toResponseList(devices)
                }
        } catch (e: Exception) {
            logger.error("Error occurred while fetching all devices", e)
            DomainError.DeviceError("DEVICES_FETCH_ERROR", "Failed to fetch devices: ${e.message}").left()
        }
    }
}
