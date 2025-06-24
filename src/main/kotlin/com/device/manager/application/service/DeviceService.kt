package com.device.manager.application.service

import arrow.core.Either
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.CreateDeviceUseCase
import com.device.manager.domain.port.inbound.DeleteDeviceUseCase
import com.device.manager.domain.port.inbound.GetAllDevicesUseCase
import com.device.manager.domain.port.inbound.GetDeviceUseCase
import com.device.manager.domain.port.inbound.GetDevicesByBrandUseCase
import com.device.manager.domain.port.inbound.GetDevicesByStateUseCase
import com.device.manager.domain.port.inbound.UpdateDeviceUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeviceService(
    private val createDeviceUseCase: CreateDeviceUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
    private val getDeviceUseCase: GetDeviceUseCase,
    private val getAllDevicesUseCase: GetAllDevicesUseCase,
    private val getDevicesByBrandUseCase: GetDevicesByBrandUseCase,
    private val getDevicesByStateUseCase: GetDevicesByStateUseCase,
    private val deleteDeviceUseCase: DeleteDeviceUseCase
) {

    private val logger = LoggerFactory.getLogger(DeviceService::class.java)

    // Create a new device
    suspend fun execute(request: CreateDeviceRequest): Either<DomainError.DeviceError, DeviceResponse> {
        logger.debug("DeviceService delegating create request to CreateDeviceUseCase")
        return createDeviceUseCase.execute(request)
    }

    // Fully and/or partially update an existing device
    suspend fun execute(id: UUID, request: UpdateDeviceRequest): Either<DomainError.DeviceError, DeviceResponse> {
        logger.debug("DeviceService delegating update request for device {} to UpdateDeviceUseCase", id)
        return updateDeviceUseCase.execute(id, request)
    }

    // Fetch a single device
    suspend fun execute(id: UUID): Either<DomainError.DeviceError, DeviceResponse> {
        logger.debug("DeviceService delegating get request for device {} to GetDeviceUseCase", id)
        return getDeviceUseCase.execute(id)
    }

    // Fetch all devices
    suspend fun execute(): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.debug("DeviceService delegating get all devices request to GetAllDevicesUseCase")
        return getAllDevicesUseCase.execute()
    }

    // Fetch devices by brand
    suspend fun execute(brand: String): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.debug("DeviceService delegating get devices by brand '{}' request to GetDevicesByBrandUseCase", brand)
        return getDevicesByBrandUseCase.execute(brand)
    }

    // Fetch devices by state
    suspend fun execute(state: DeviceState): Either<DomainError.DeviceError, List<DeviceResponse>> {
        logger.debug("DeviceService delegating get devices by state '{}' request to GetDevicesByStateUseCase", state)
        return getDevicesByStateUseCase.execute(state)
    }

    // Delete a single device
    suspend fun deleteDevice(id: UUID): Either<DomainError.DeviceError, Unit> {
        logger.debug("DeviceService delegating delete request for device {} to DeleteDeviceUseCase", id)
        return deleteDeviceUseCase.execute(id)
    }
}
