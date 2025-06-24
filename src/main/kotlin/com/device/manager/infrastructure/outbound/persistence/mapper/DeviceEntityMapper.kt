package com.device.manager.infrastructure.outbound.persistence.mapper

import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import com.device.manager.infrastructure.outbound.persistence.entity.DeviceEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DeviceEntityMapper {

    private val logger = LoggerFactory.getLogger(DeviceEntityMapper::class.java)

    fun toDomain(entity: DeviceEntity): Device {
        logger.debug("Mapping DeviceEntity to Device domain: {} - {}", entity.name, entity.brand)
        return Device(
            id = entity.id!!,
            name = entity.name,
            type = entity.type,
            brand = entity.brand,
            state = DeviceState.of(entity.state),
            creationTime = entity.createdAt,
            updatedTime = entity.updatedAt
        ).also {
            logger.debug("Mapped to Device domain: {}", it.id)
        }
    }

    fun toEntity(domain: Device): DeviceEntity {
        logger.debug("Mapping Device domain to DeviceEntity: {} - {}", domain.name, domain.brand)
        return DeviceEntity(
            id = domain.id,
            name = domain.name,
            type = domain.type,
            brand = domain.brand,
            state = domain.state.state,
            createdAt = domain.creationTime,
            updatedAt = domain.updatedTime
        ).also {
            logger.debug("Mapped to DeviceEntity: {}", it.id)
        }
    }

    fun toEntityList(domains: List<Device>): List<DeviceEntity> {
        logger.debug("Mapping {} Device domains to DeviceEntity list", domains.size)
        return domains.map { toEntity(it) }.also {
            logger.debug("Mapped {} domains to entity list", it.size)
        }
    }

    fun toDomainList(entities: List<DeviceEntity>): List<Device> {
        logger.debug("Mapping {} DeviceEntity to Device domain list", entities.size)
        return entities.map { toDomain(it) }.also {
            logger.debug("Mapped {} entities to domain list", it.size)
        }
    }
}
