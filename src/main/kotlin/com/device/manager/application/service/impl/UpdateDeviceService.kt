package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.UpdateDeviceUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UpdateDeviceService(
    private val deviceRepository: DeviceRepository
) : UpdateDeviceUseCase {

    private val logger = LoggerFactory.getLogger(UpdateDeviceService::class.java)

    override suspend fun execute(id: UUID, request: UpdateDeviceRequest): Either<DomainError.DeviceError, DeviceResponse> {
        logger.info("Executing update device service for device ID: {}", id)
        logger.debug("Update device request: {}", request)

        return try {
            logger.debug("Fetching existing device with ID: {}", id)
            deviceRepository.findById(id)
                .flatMap { existingDevice ->
                    logger.debug("Found existing device: {} - {}", existingDevice.name, existingDevice.brand)

                    val updatedDevice = existingDevice.copy(
                        name = request.name ?: existingDevice.name,
                        type = request.type ?: existingDevice.type,
                        brand = request.brand ?: existingDevice.brand,
                        state = request.state ?: existingDevice.state,
                        updatedTime = ZonedDateTime.now()
                    )

                    logger.debug("Updating device with new values: name={}, type={}, brand={}, state={}",
                        updatedDevice.name, updatedDevice.type, updatedDevice.brand, updatedDevice.state)

                    deviceRepository.update(id, updatedDevice)
                        .map { savedDevice ->
                            logger.info("Device updated successfully: {}", savedDevice.id)
                            DeviceMapper.toResponse(savedDevice)
                        }
                }
        } catch (e: Exception) {
            logger.error("Error occurred while updating device with ID: {}", id, e)
            DomainError.DeviceError("DEVICE_UPDATE_ERROR", "Failed to update device: ${e.message}").left()
        }
    }
}
