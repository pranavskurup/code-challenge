package com.device.manager.domain.entity

enum class DeviceState(val state: String) {
    AVAILABLE("available"),
    IN_USE("in-use"),
    INACTIVE("inactive");

    companion object {
        fun of(state: String): DeviceState {
            return entries.firstOrNull { it.state == state } ?: AVAILABLE
        }
    }

}
