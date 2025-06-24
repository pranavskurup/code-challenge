package com.device.manager.domain.port.inbound

import arrow.core.Either
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError.DeviceError
import java.util.UUID

interface CreateDeviceUseCase {
    suspend fun execute(request: CreateDeviceRequest): Either<DeviceError, DeviceResponse>
}

interface UpdateDeviceUseCase {
    suspend fun execute(id: UUID, request: UpdateDeviceRequest): Either<DeviceError, DeviceResponse>
}

interface GetDeviceUseCase {
    suspend fun execute(id: UUID): Either<DeviceError, DeviceResponse>
}

interface GetAllDevicesUseCase {
    suspend fun execute(): Either<DeviceError, List<DeviceResponse>>
}

interface GetDevicesByBrandUseCase {
    suspend fun execute(brand: String): Either<DeviceError, List<DeviceResponse>>
}

interface GetDevicesByStateUseCase {
    suspend fun execute(state: DeviceState): Either<DeviceError, List<DeviceResponse>>
}

interface DeleteDeviceUseCase {
    suspend fun execute(id: UUID): Either<DeviceError, Unit>
}
