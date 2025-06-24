package com.device.manager.domain.entity

import arrow.core.left
import arrow.core.right
import com.device.manager.domain.errors.DomainError.DeviceError.DeviceStateChangeError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeviceTest {

    private val deviceId = UUID.randomUUID()
    private val deviceName = "Test Device"
    private val deviceType = "Smartphone"
    private val deviceBrand = "Smartphone Organization"
    private val fixedTime = ZonedDateTime.now()

    @Test
    fun `should create device with default values`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType
        )

        assertEquals(deviceId, device.id)
        assertEquals(deviceName, device.name)
        assertEquals(deviceType, device.type)
        assertEquals(DeviceState.INACTIVE, device.state)
        assertTrue { device.creationTime.isBefore(ZonedDateTime.now().plusSeconds(1)) }
        assertTrue { device.updatedTime.isBefore(ZonedDateTime.now().plusSeconds(1)) }
    }

    @Test
    fun `should create device with custom values`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        assertEquals(deviceId, device.id)
        assertEquals(deviceName, device.name)
        assertEquals(deviceType, device.type)
        assertEquals(DeviceState.AVAILABLE, device.state)
        assertEquals(fixedTime, device.creationTime)
        assertEquals(fixedTime, device.updatedTime)
    }

    @Test
    fun `isAvailable should return true when device state is AVAILABLE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE
        )

        assertTrue(device.isAvailable())
        assertFalse(device.isInUse())
        assertFalse(device.isInactive())
    }

    @Test
    fun `isInUse should return true when device state is IN_USE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.IN_USE
        )

        assertFalse(device.isAvailable())
        assertTrue(device.isInUse())
        assertFalse(device.isInactive())
    }

    @Test
    fun `isInactive should return true when device state is INACTIVE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.INACTIVE
        )

        assertFalse(device.isAvailable())
        assertFalse(device.isInUse())
        assertTrue(device.isInactive())
    }

    @Test
    fun `toAvailable should succeed when device is INACTIVE`() = runTest {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val result = device.toAvailable()

        assertTrue(result.isRight())
        result.fold(
            { error -> kotlin.test.fail("Expected success but got error: $error") },
            { updatedDevice ->
                assertEquals(DeviceState.AVAILABLE, updatedDevice.state)
                assertEquals(fixedTime, updatedDevice.creationTime)
                assertTrue(updatedDevice.updatedTime.isAfter(fixedTime))
            }
        )
    }

    @Test
    fun `toAvailable should fail when device is AVAILABLE`() = runTest {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE
        )

        val result = device.toAvailable()

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is DeviceStateChangeError)
                assertEquals(deviceId.toString(), error.deviceId)
                assertEquals(DeviceState.AVAILABLE, error.currentState)
            },
            { kotlin.test.fail("Expected error but got success") }
        )
    }

    @Test
    fun `toAvailable should fail when device is IN_USE`() = runTest {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.IN_USE
        )

        val result = device.toAvailable()

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is DeviceStateChangeError)
                assertEquals(deviceId.toString(), error.deviceId)
                assertEquals(DeviceState.IN_USE, error.currentState)
            },
            { kotlin.test.fail("Expected error but got success") }
        )
    }

    @Test
    fun `toInUse should succeed when device is AVAILABLE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val result = device.toInUse()

        assertTrue(result.isRight())
        result.fold(
            { error -> kotlin.test.fail("Expected success but got error: $error") },
            { updatedDevice ->
                assertEquals(DeviceState.IN_USE, updatedDevice.state)
                assertEquals(fixedTime, updatedDevice.creationTime)
                assertTrue(updatedDevice.updatedTime.isAfter(fixedTime))
            }
        )
    }

    @Test
    fun `toInUse should fail when device is INACTIVE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.INACTIVE
        )

        val result = device.toInUse()

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is DeviceStateChangeError)
                assertEquals(deviceId.toString(), error.deviceId)
                assertEquals(DeviceState.INACTIVE, error.currentState)
            },
            { kotlin.test.fail("Expected error but got success") }
        )
    }

    @Test
    fun `toInUse should fail when device is IN_USE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.IN_USE
        )

        val result = device.toInUse()

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is DeviceStateChangeError)
                assertEquals(deviceId.toString(), error.deviceId)
                assertEquals(DeviceState.IN_USE, error.currentState)
            },
            { kotlin.test.fail("Expected error but got success") }
        )
    }

    @Test
    fun `toInactive should succeed when device is not INACTIVE`() {
        val availableDevice = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val result = availableDevice.toInactive()

        assertTrue(result.isRight())
        result.fold(
            { error -> kotlin.test.fail("Expected success but got error: $error") },
            { updatedDevice ->
                assertEquals(DeviceState.INACTIVE, updatedDevice.state)
                assertEquals(fixedTime, updatedDevice.creationTime)
                assertTrue(updatedDevice.updatedTime.isAfter(fixedTime))
            }
        )

        // Test with IN_USE state as well
        val inUseDevice = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val inUseResult = inUseDevice.toInactive()

        assertTrue(inUseResult.isRight())
        inUseResult.fold(
            { error -> kotlin.test.fail("Expected success but got error: $error") },
            { updatedDevice ->
                assertEquals(DeviceState.INACTIVE, updatedDevice.state)
                assertEquals(fixedTime, updatedDevice.creationTime)
                assertTrue(updatedDevice.updatedTime.isAfter(fixedTime))
            }
        )
    }

    @Test
    fun `toInactive should fail when device is already INACTIVE`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.INACTIVE
        )

        val result = device.toInactive()

        assertTrue(result.isLeft())
        result.fold(
            { error ->
                assertTrue(error is DeviceStateChangeError)
                assertEquals(deviceId.toString(), error.deviceId)
                assertEquals(DeviceState.INACTIVE, error.currentState)
            },
            { kotlin.test.fail("Expected error but got success") }
        )
    }

    @Test
    fun `toString should return formatted string representation`() {
        val device = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE
        )

        val expectedString = "Device(id=$deviceId, name='$deviceName', type='$deviceType', brand='$deviceBrand', status='${DeviceState.AVAILABLE}')"
        assertEquals(expectedString, device.toString())
    }

    @Test
    fun `copy should create new device with updated fields`() {
        val originalDevice = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val newTime = ZonedDateTime.now()
        val copiedDevice = originalDevice.copy(
            state = DeviceState.AVAILABLE,
            updatedTime = newTime
        )

        assertEquals(originalDevice.id, copiedDevice.id)
        assertEquals(originalDevice.name, copiedDevice.name)
        assertEquals(originalDevice.type, copiedDevice.type)
        assertEquals(originalDevice.creationTime, copiedDevice.creationTime)
        assertEquals(DeviceState.AVAILABLE, copiedDevice.state)
        assertEquals(newTime, copiedDevice.updatedTime)
    }

    @Test
    fun `equality should work correctly for data class`() {
        val device1 = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val device2 = Device(
            id = deviceId,
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val device3 = Device(
            id = UUID.randomUUID(),
            name = deviceName,
            brand = deviceBrand,
            type = deviceType,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        assertEquals(device1, device2)
        assertNotEquals(device1, device3)
        assertEquals(device1.hashCode(), device2.hashCode())
        assertNotEquals(device1.hashCode(), device3.hashCode())
    }
}
