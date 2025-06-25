package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.GetDevicesByBrandUseCase
import com.device.manager.domain.port.outbound.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetDevicesByBrandService(
    private val deviceRepository: DeviceRepository
) : GetDevicesByBrandUseCase {

    private val logger = LoggerFactory.getLogger(GetDevicesByBrandService::class.java)

    override suspend fun execute(brand: String): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.info("Executing get devices by brand service for brand: {}", brand)

        return try {
            if (brand.isBlank()) {
                logger.warn("Empty or blank brand parameter provided")
                DomainError.DeviceError("INVALID_BRAND", "Brand cannot be empty").left()
            } else {
                logger.debug("Fetching devices from repository with brand: {}", brand)
                deviceRepository.findByBrand(brand)
                    .map { devices ->
                        logger.info("Successfully retrieved {} devices for brand: {}", devices.size, brand)
                        logger.debug("Devices for brand {}: {}", brand, devices.map { "${it.name} (${it.id})" })
                        DeviceMapper.toResponseList(devices)
                    }
            }
        } catch (e: Exception) {
            logger.error("Error occurred while fetching devices by brand: {}", brand, e)
            DomainError.DeviceError("DEVICES_BY_BRAND_FETCH_ERROR", "Failed to fetch devices by brand: ${e.message}").left()
        }
    }
}
