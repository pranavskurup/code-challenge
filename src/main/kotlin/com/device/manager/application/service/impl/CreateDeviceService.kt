package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.CreateDeviceUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CreateDeviceService(
    private val deviceRepository: DeviceRepository
) : CreateDeviceUseCase {

    private val logger = LoggerFactory.getLogger(CreateDeviceService::class.java)

    override suspend fun execute(request: CreateDeviceRequest): Either<DomainError.DeviceError, DeviceResponse> {
        logger.info("Executing create device service for device: {} - {}", request.name, request.brand)
        logger.debug("Create device request details: {}", request)
        
        return try {
            val device = DeviceMapper.toEntity(request)
            logger.debug("Mapped request to device entity: {}", device.id)

            // Check if device with same name and brand already exists
            logger.debug("Checking for existing devices with brand: {}", request.brand)
            deviceRepository.findByBrand(request.brand)
                .flatMap { devices ->
                    logger.debug("Found {} existing devices with brand: {}", devices.size, request.brand)
                    
                    val existingDevice = devices.find { it.name == request.name }
                    if (existingDevice != null) {
                        logger.warn("Device already exists with name '{}' and brand '{}', ID: {}", 
                            request.name, request.brand, existingDevice.id)
                        DomainError.DeviceError.DeviceAlreadyExists("Device with name '${request.name}' and brand '${request.brand}' already exists")
                            .left()
                    } else {
                        logger.debug("No duplicate device found, proceeding with save")
                        deviceRepository.save(device)
                            .map { savedDevice -> 
                                logger.info("Device created successfully with ID: {}", savedDevice.id)
                                DeviceMapper.toResponse(savedDevice) 
                            }
                    }
                }
        } catch (e: Exception) {
            logger.error("Error occurred while creating device: {} - {}", request.name, request.brand, e)
            DomainError.DeviceError("DEVICE_CREATION_ERROR", "Failed to create device: ${e.message}").left()
        }
    }
}
