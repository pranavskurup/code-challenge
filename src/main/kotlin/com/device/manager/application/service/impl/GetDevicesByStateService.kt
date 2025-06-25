package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.GetDevicesByStateUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetDevicesByStateService(
    private val deviceRepository: DeviceRepository
) : GetDevicesByStateUseCase {

    private val logger = LoggerFactory.getLogger(GetDevicesByStateService::class.java)

    override suspend fun execute(state: DeviceState): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.info("Executing get devices by state service for state: {}", state)

        return try {
            logger.debug("Fetching devices from repository with state: {}", state)
            deviceRepository.findByState(state)
                .map { devices ->
                    logger.info("Successfully retrieved {} devices with state: {}", devices.size, state)
                    logger.debug("Devices with state {}: {}", state, devices.map { "${it.name} (${it.id})" })
                    DeviceMapper.toResponseList(devices)
                }
        } catch (e: Exception) {
            logger.error("Error occurred while fetching devices by state: {}", state, e)
            DomainError.DeviceError("DEVICES_BY_STATE_FETCH_ERROR", "Failed to fetch devices by state: ${e.message}").left()
        }
    }
}
