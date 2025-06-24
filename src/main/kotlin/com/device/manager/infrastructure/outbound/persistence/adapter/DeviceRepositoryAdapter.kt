package com.device.manager.infrastructure.outbound.persistence.adapter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError.DeviceError
import com.device.manager.domain.errors.DomainError.DeviceError.DeviceNotFound
import com.device.manager.domain.errors.DomainError.DeviceError.CreationTimeImmutable
import com.device.manager.domain.errors.DomainError.DeviceError.InUseDeviceImmutableFields
import com.device.manager.domain.errors.DomainError.DeviceError.InUseDeviceCannotBeDeleted
import com.device.manager.domain.port.outbound.DeviceRepository
import com.device.manager.infrastructure.outbound.persistence.mapper.DeviceEntityMapper
import com.device.manager.infrastructure.outbound.persistence.repository.DeviceR2dbcRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
@Transactional(readOnly = true)
class DeviceRepositoryAdapter(
    private val deviceR2dbcRepository: DeviceR2dbcRepository,
    private val deviceEntityMapper: DeviceEntityMapper
) : DeviceRepository {

    private val logger = LoggerFactory.getLogger(DeviceRepositoryAdapter::class.java)

    @Transactional
    override suspend fun save(device: Device): Either<DeviceError, Device> {
        logger.info("Saving device: {} - {}", device.name, device.brand)
        logger.debug("Device details for save: {}", device)
        
        return try {
            val entity = deviceEntityMapper.toEntity(device)
            logger.debug("Mapped device to entity: {}", entity.id)
            
            val savedEntity = deviceR2dbcRepository.save(entity).awaitSingle()
            val savedDevice = deviceEntityMapper.toDomain(savedEntity)
            
            logger.info("Device saved successfully with ID: {}", savedDevice.id)
            savedDevice.right()
        } catch (e: Exception) {
            logger.error("Failed to save device: {} - {}", device.name, device.brand, e)
            DeviceError("DEVICE_SAVE_ERROR", "Failed to save device: ${e.message}").left()
        }
    }

    override suspend fun findById(id: UUID): Either<DeviceError, Device> {
        logger.debug("Finding device by ID: {}", id)
        
        return try {
            val entity = deviceR2dbcRepository.findById(id).awaitSingleOrNull()
            if (entity != null) {
                val device = deviceEntityMapper.toDomain(entity)
                logger.debug("Found device: {} - {}", device.name, device.brand)
                device.right()
            } else {
                logger.warn("Device not found with ID: {}", id)
                DeviceNotFound("Device with id $id not found").left()
            }
        } catch (e: Exception) {
            logger.error("Error finding device by ID: {}", id, e)
            DeviceError("DEVICE_FIND_ERROR", "Failed to find device: ${e.message}").left()
        }
    }

    override suspend fun findAll(): Either<DeviceError, List<Device>> {
        logger.debug("Finding all devices")
        
        return try {
            val entities = deviceR2dbcRepository.findAll().collectList().awaitSingle()
            val devices = deviceEntityMapper.toDomainList(entities)
            logger.info("Found {} devices in total", devices.size)
            logger.debug("Devices summary: {}", devices.groupBy { it.state }.mapValues { it.value.size })
            devices.right()
        } catch (e: Exception) {
            logger.error("Error finding all devices", e)
            DeviceError("DEVICES_FIND_ERROR", "Failed to find devices: ${e.message}").left()
        }
    }

    override suspend fun findByBrand(brand: String): Either<DeviceError, List<Device>> {
        logger.debug("Finding devices by brand: {}", brand)
        
        return try {
            val entities = deviceR2dbcRepository.findByBrand(brand).collectList().awaitSingle()
            val devices = deviceEntityMapper.toDomainList(entities)
            logger.info("Found {} devices for brand: {}", devices.size, brand)
            logger.debug("Devices for brand {}: {}", brand, devices.map { "${it.name} (${it.id})" })
            devices.right()
        } catch (e: Exception) {
            logger.error("Error finding devices by brand: {}", brand, e)
            DeviceError("DEVICES_FIND_BY_BRAND_ERROR", "Failed to find devices by brand: ${e.message}").left()
        }
    }

    override suspend fun findByState(state: DeviceState): Either<DeviceError, List<Device>> {
        logger.debug("Finding devices by state: {}", state)
        
        return try {
            val entities = deviceR2dbcRepository.findByState(state.state).collectList().awaitSingle()
            val devices = deviceEntityMapper.toDomainList(entities)
            logger.info("Found {} devices with state: {}", devices.size, state)
            logger.debug("Devices with state {}: {}", state, devices.map { "${it.name} (${it.id})" })
            devices.right()
        } catch (e: Exception) {
            logger.error("Error finding devices by state: {}", state, e)
            DeviceError("DEVICES_FIND_BY_STATE_ERROR", "Failed to find devices by state: ${e.message}").left()
        }
    }

    @Transactional
    override suspend fun update(id: UUID, device: Device): Either<DeviceError, Device> {
        logger.info("Updating device with ID: {}", id)
        logger.debug("Update device details: {}", device)
        
        return try {
            // First find the existing device to check business rules
            findById(id).fold(
                { error -> 
                    logger.warn("Cannot update device - device not found: {}", id)
                    error.left() 
                },
                { existingDevice ->
                    logger.debug("Found existing device for update: {} - {}", existingDevice.name, existingDevice.brand)
                    
                    // Business Rule: Creation time cannot be updated
                    if (device.creationTime != existingDevice.creationTime) {
                        logger.warn("Attempted to update creation time for device: {}", id)
                        CreationTimeImmutable().left()
                    }
                    // Business Rule: Name and brand cannot be updated if device is in use
                    else if (existingDevice.isInUse() &&
                        (device.name != existingDevice.name || device.brand != existingDevice.brand)
                    ) {
                        logger.warn("Attempted to update immutable fields (name/brand) for in-use device: {}", id)
                        InUseDeviceImmutableFields().left()
                    } else {
                        // Update the device with the provided id and preserve creation time
                        val deviceWithId = device.copy(
                            id = id,
                            creationTime = existingDevice.creationTime
                        )
                        logger.debug("Proceeding with device update: {}", deviceWithId)
                        
                        val entity = deviceEntityMapper.toEntity(deviceWithId)
                        val updatedEntity = deviceR2dbcRepository.save(entity).awaitSingle()
                        val updatedDevice = deviceEntityMapper.toDomain(updatedEntity)
                        
                        logger.info("Device updated successfully: {}", updatedDevice.id)
                        updatedDevice.right()
                    }
                }
            )
        } catch (e: Exception) {
            logger.error("Error updating device with ID: {}", id, e)
            DeviceError("DEVICE_UPDATE_ERROR", "Failed to update device: ${e.message}").left()
        }
    }

    @Transactional
    override suspend fun deleteById(id: UUID): Either<DeviceError, Unit> {
        logger.info("Deleting device with ID: {}", id)
        
        return try {
            // First find the device to check business rules
            findById(id).fold(
                { error -> 
                    logger.warn("Cannot delete device - device not found: {}", id)
                    error.left() 
                },
                { existingDevice ->
                    logger.debug("Found device for deletion: {} - {}", existingDevice.name, existingDevice.brand)
                    
                    // Business Rule: In use devices cannot be deleted
                    if (existingDevice.isInUse()) {
                        logger.warn("Attempted to delete in-use device: {} ({})", existingDevice.name, id)
                        InUseDeviceCannotBeDeleted().left()
                    } else {
                        logger.debug("Proceeding with device deletion: {}", id)
                        deviceR2dbcRepository.deleteById(id).awaitSingleOrNull()
                        logger.info("Device deleted successfully: {}", id)
                        Unit.right()
                    }
                }
            )
        } catch (e: Exception) {
            logger.error("Error deleting device with ID: {}", id, e)
            DeviceError("DEVICE_DELETE_ERROR", "Failed to delete device: ${e.message}").left()
        }
    }

    override suspend fun existsById(id: UUID): Either<DeviceError, Boolean> {
        logger.debug("Checking if device exists with ID: {}", id)
        
        return try {
            val exists = deviceR2dbcRepository.existsById(id).awaitSingle()
            logger.debug("Device exists check result for ID {}: {}", id, exists)
            exists.right()
        } catch (e: Exception) {
            logger.error("Error checking if device exists with ID: {}", id, e)
            DeviceError("DEVICE_EXISTS_ERROR", "Failed to check if device exists: ${e.message}").left()
        }
    }
}
