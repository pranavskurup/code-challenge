package com.device.manager.domain.entity

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.domain.errors.DomainError.DeviceError.DeviceStateChangeError
import java.time.ZonedDateTime
import java.util.UUID

data class Device(
    val id: UUID? = null,
    val name: String,
    val type: String,
    val brand: String,
    val state: DeviceState = DeviceState.INACTIVE,
    val creationTime: ZonedDateTime = ZonedDateTime.now(),
    val updatedTime: ZonedDateTime = ZonedDateTime.now()
) {


    fun isAvailable(): Boolean {
        return state == DeviceState.AVAILABLE
    }

    fun isInUse(): Boolean {
        return state == DeviceState.IN_USE
    }

    fun isInactive(): Boolean {
        return state == DeviceState.INACTIVE
    }

    suspend fun toAvailable(): Either<DeviceStateChangeError,Device> {
        if (state != DeviceState.INACTIVE) {
            return DeviceStateChangeError(id.toString(), state).left()
        }
        return this.copy(state = DeviceState.AVAILABLE, updatedTime = ZonedDateTime.now()).right()
    }

    fun toInUse(): Either<DeviceStateChangeError,Device> {
        if (state != DeviceState.AVAILABLE) {
            return DeviceStateChangeError(id.toString(), state).left()
        }
        return this.copy(state = DeviceState.IN_USE, updatedTime = ZonedDateTime.now()).right()
    }

    fun toInactive(): Either<DeviceStateChangeError,Device> {
        if (state == DeviceState.INACTIVE) {
            return DeviceStateChangeError(id.toString(), state).left()
        }
        return this.copy(state = DeviceState.INACTIVE, updatedTime = ZonedDateTime.now()).right()
    }

    override fun toString(): String {
        return "Device(id=$id, name='$name', type='$type', brand='$brand', status='$state')"
    }
}
