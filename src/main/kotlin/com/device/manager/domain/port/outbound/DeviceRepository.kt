package com.device.manager.domain.port.outbound

import arrow.core.Either
import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError.DeviceError
import java.util.UUID

interface DeviceRepository {
    suspend fun save(device: Device): Either<DeviceError, Device>
    suspend fun findById(id: UUID): Either<DeviceError, Device>
    suspend fun findAll(): Either<DeviceError, List<Device>>
    suspend fun findByBrand(brand: String): Either<DeviceError, List<Device>>
    suspend fun findByState(state: DeviceState): Either<DeviceError, List<Device>>
    suspend fun update(id: UUID, device: Device): Either<DeviceError, Device>
    suspend fun deleteById(id: UUID): Either<DeviceError, Unit>
    suspend fun existsById(id: UUID): Either<DeviceError, Boolean>
}
