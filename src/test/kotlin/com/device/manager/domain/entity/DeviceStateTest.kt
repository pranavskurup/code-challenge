package com.device.manager.domain.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeviceStateTest {

    @Test
    fun `should create DeviceState with correct state values`() {
        assertEquals("available", DeviceState.AVAILABLE.state)
        assertEquals("in-use", DeviceState.IN_USE.state)
        assertEquals("inactive", DeviceState.INACTIVE.state)
    }

    @Test
    fun `should return AVAILABLE for valid 'available' state string`() {
        val result = DeviceState.of("available")
        assertEquals(DeviceState.AVAILABLE, result)
    }

    @Test
    fun `should return IN_USE for valid 'in-use' state string`() {
        val result = DeviceState.of("in-use")
        assertEquals(DeviceState.IN_USE, result)
    }

    @Test
    fun `should return INACTIVE for valid 'inactive' state string`() {
        val result = DeviceState.of("inactive")
        assertEquals(DeviceState.INACTIVE, result)
    }

    @Test
    fun `should return AVAILABLE for invalid state string`() {
        val result = DeviceState.of("invalid-state")
        assertEquals(DeviceState.AVAILABLE, result)
    }

    @Test
    fun `should return AVAILABLE for empty state string`() {
        val result = DeviceState.of("")
        assertEquals(DeviceState.AVAILABLE, result)
    }

    @Test
    fun `should return AVAILABLE for null-like state string`() {
        val result = DeviceState.of("null")
        assertEquals(DeviceState.AVAILABLE, result)
    }

    @Test
    fun `should handle case sensitivity correctly`() {
        val upperCaseResult = DeviceState.of("AVAILABLE")
        val mixedCaseResult = DeviceState.of("Available")
        
        // Should return AVAILABLE as default since case doesn't match
        assertEquals(DeviceState.AVAILABLE, upperCaseResult)
        assertEquals(DeviceState.AVAILABLE, mixedCaseResult)
    }

    @Test
    fun `should have three enum values`() {
        val values = DeviceState.entries
        assertEquals(3, values.size)
        assertEquals(setOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE), values.toSet())
    }

    @Test
    fun `should maintain enum order`() {
        val values = DeviceState.entries
        assertEquals(DeviceState.AVAILABLE, values[0])
        assertEquals(DeviceState.IN_USE, values[1])
        assertEquals(DeviceState.INACTIVE, values[2])
    }
}
