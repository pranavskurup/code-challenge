package com.device.manager.infrastructure.outbound.persistence.repository

import com.device.manager.infrastructure.outbound.persistence.entity.DeviceEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.ZonedDateTime
import java.util.UUID

interface DeviceR2dbcRepository : ReactiveCrudRepository<DeviceEntity, UUID> {

    @Query("SELECT * FROM devices WHERE brand = :brand")
    fun findByBrand(brand: String): Flux<DeviceEntity>

    @Query("SELECT * FROM devices WHERE state = :state")
    fun findByState(state: String): Flux<DeviceEntity>
}
