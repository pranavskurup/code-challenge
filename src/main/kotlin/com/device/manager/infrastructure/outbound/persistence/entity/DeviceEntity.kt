package com.device.manager.infrastructure.outbound.persistence.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
import java.util.UUID

@Table("devices")
data class DeviceEntity(
    @Id
    val id: UUID? = null,

    @Column("name")
    val name: String,

    @Column("type")
    val type: String,

    @Column("brand")
    val brand: String,

    @Column("state")
    val state: String,

    @Column("created_at")
    val createdAt: ZonedDateTime,

    @Column("updated_at")
    val updatedAt: ZonedDateTime
)
