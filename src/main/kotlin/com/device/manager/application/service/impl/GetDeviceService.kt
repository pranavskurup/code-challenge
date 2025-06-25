package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.GetDeviceUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetDeviceService(
    private val deviceRepository: DeviceRepository
) : GetDeviceUseCase {

    private val logger = LoggerFactory.getLogger(GetDeviceService::class.java)

    override suspend fun execute(id: UUID): Either<DomainError.DeviceError, DeviceResponse> {
        logger.info("Executing get device service for device ID: {}", id)

        return try {
            logger.debug("Fetching device from repository with ID: {}", id)
            deviceRepository.findById(id)
                .map { device ->
                    logger.debug("Successfully found device: {} - {}", device.name, device.brand)
                    DeviceMapper.toResponse(device)
                }
        } catch (e: Exception) {
            logger.error("Error occurred while fetching device with ID: {}", id, e)
            DomainError.DeviceError("DEVICE_FETCH_ERROR", "Failed to fetch device: ${e.message}").left()
        }
    }
}
