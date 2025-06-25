package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.DeleteDeviceUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeleteDeviceService(
    private val deviceRepository: DeviceRepository
) : DeleteDeviceUseCase {

    private val logger = LoggerFactory.getLogger(DeleteDeviceService::class.java)

    override suspend fun execute(id: UUID): Either<DomainError.DeviceError, Unit> {
        logger.info("Executing delete device service for device ID: {}", id)

        return try {
            logger.debug("Checking if device exists with ID: {}", id)
            deviceRepository.existsById(id)
                .flatMap { exists ->
                    if (exists) {
                        logger.debug("Device exists, proceeding with deletion: {}", id)
                        deviceRepository.deleteById(id)
                            .map {
                                logger.info("Device deleted successfully: {}", id)
                                it
                            }
                    } else {
                        logger.warn("Attempted to delete non-existent device with ID: {}", id)
                        DomainError.DeviceError.DeviceNotFound("Device with id $id not found").left()
                    }
                }
        } catch (e: Exception) {
            logger.error("Error occurred while deleting device with ID: {}", id, e)
            DomainError.DeviceError("DEVICE_DELETE_ERROR", "Failed to delete device: ${e.message}").left()
        }
    }
}
