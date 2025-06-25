package com.device.manager.domain.errors

import com.device.manager.domain.entity.DeviceState

interface DomainError {
    val code: String?
    val message: String?

    open class DeviceError(override val code: String?, override val message: String?) : DomainError {
        companion object {
            const val DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND"
            const val DEVICE_ALREADY_EXISTS = "DEVICE_ALREADY_EXISTS"
            const val DEVICE_UNAUTHORIZED = "DEVICE_UNAUTHORIZED"
            const val DEVICE_STATE_CHANGE_ERROR = "DEVICE_STATE_CHANGE_ERROR"
            const val CREATION_TIME_IMMUTABLE = "CREATION_TIME_IMMUTABLE"
            const val IN_USE_DEVICE_IMMUTABLE_FIELDS = "IN_USE_DEVICE_IMMUTABLE_FIELDS"
            const val IN_USE_DEVICE_CANNOT_BE_DELETED = "IN_USE_DEVICE_CANNOT_BE_DELETED"
        }

        data class DeviceNotFound(override val message: String? = "Device not found") :
            DeviceError(DEVICE_NOT_FOUND, message)

        data class DeviceAlreadyExists(override val message: String? = "Device already exists") :
            DeviceError(DEVICE_ALREADY_EXISTS, message)

        data class DeviceUnauthorized(override val message: String? = "Device is not authorized") :
            DeviceError(DEVICE_UNAUTHORIZED, message)

        data class DeviceStateChangeError(val deviceId: String, val currentState: DeviceState) :
            DeviceError(DEVICE_STATE_CHANGE_ERROR, "Failed to change device state for device $deviceId from $currentState")

        data class CreationTimeImmutable(override val message: String? = "Creation time cannot be updated") :
            DeviceError(CREATION_TIME_IMMUTABLE, message)

        data class InUseDeviceImmutableFields(override val message: String? = "Name and brand cannot be updated when device is in use") :
            DeviceError(IN_USE_DEVICE_IMMUTABLE_FIELDS, message)

        data class InUseDeviceCannotBeDeleted(override val message: String? = "Device cannot be deleted while it is in use") :
            DeviceError(IN_USE_DEVICE_CANNOT_BE_DELETED, message)
    }
}
