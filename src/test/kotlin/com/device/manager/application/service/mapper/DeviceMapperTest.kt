package com.device.manager.application.service.mapper

import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class DeviceMapperTest {

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")
    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @BeforeEach
    fun setUp() {
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns fixedTime
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `toEntity should map CreateDeviceRequest to Device entity correctly`() {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        // When
        val result = DeviceMapper.toEntity(request)

        // Then
        assertNotNull(result)
        assertEquals(request.name, result.name)
        assertEquals(request.type, result.type)
        assertEquals(request.brand, result.brand)
        assertEquals(request.state, result.state)
        assertEquals(fixedTime, result.creationTime)
        assertEquals(fixedTime, result.updatedTime)
        assertNull(result.id) // ID should be null for new entities
    }

    @Test
    fun `toEntity should use default INACTIVE state when not specified`() {
        // Given
        val request = CreateDeviceRequest(
            name = "MacBook Pro",
            type = "laptop",
            brand = "Apple"
            // state not specified, should default to INACTIVE
        )

        // When
        val result = DeviceMapper.toEntity(request)

        // Then
        assertEquals(DeviceState.INACTIVE, result.state)
    }

    @Test
    fun `toResponse should map Device entity to DeviceResponse correctly`() {
        // Given
        val device = Device(
            id = deviceId,
            name = "Samsung Galaxy",
            type = "smartphone",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime.plusHours(1)
        )

        // When
        val result = DeviceMapper.toResponse(device)

        // Then
        assertNotNull(result)
        assertEquals(device.id, result.id)
        assertEquals(device.name, result.name)
        assertEquals(device.type, result.type)
        assertEquals(device.brand, result.brand)
        assertEquals(device.state, result.state)
        assertEquals(device.creationTime.toString(), result.creationTime)
        assertEquals(device.updatedTime.toString(), result.updatedTime)
    }

    @Test
    fun `toResponse should handle device with null id gracefully`() {
        // Given
        val device = Device(
            id = null,
            name = "Test Device",
            type = "test",
            brand = "Test Brand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        // When & Then
        // The DeviceMapper.toResponse expects a non-null id, so this should cause a null pointer exception
        // when trying to access device.id!!
        assertThrows(NullPointerException::class.java) {
            DeviceMapper.toResponse(device)
        }
    }

    @Test
    fun `toResponseList should map empty list correctly`() {
        // Given
        val devices = emptyList<Device>()

        // When
        val result = DeviceMapper.toResponseList(devices)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toResponseList should map single device list correctly`() {
        // Given
        val device = Device(
            id = deviceId,
            name = "iPad",
            type = "tablet",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )
        val devices = listOf(device)

        // When
        val result = DeviceMapper.toResponseList(devices)

        // Then
        assertEquals(1, result.size)
        val response = result.first()
        assertEquals(device.id, response.id)
        assertEquals(device.name, response.name)
        assertEquals(device.type, response.type)
        assertEquals(device.brand, response.brand)
        assertEquals(device.state, response.state)
    }

    @Test
    fun `toResponseList should map multiple devices correctly`() {
        // Given
        val device1 = Device(
            id = UUID.randomUUID(),
            name = "Device 1",
            type = "type1",
            brand = "Brand1",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )
        val device2 = Device(
            id = UUID.randomUUID(),
            name = "Device 2",
            type = "type2",
            brand = "Brand2",
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime.plusHours(2)
        )
        val devices = listOf(device1, device2)

        // When
        val result = DeviceMapper.toResponseList(devices)

        // Then
        assertEquals(2, result.size)
        
        val response1 = result.find { it.name == "Device 1" }
        assertNotNull(response1)
        assertEquals(device1.id, response1!!.id)
        assertEquals(device1.brand, response1.brand)
        
        val response2 = result.find { it.name == "Device 2" }
        assertNotNull(response2)
        assertEquals(device2.id, response2!!.id)
        assertEquals(device2.brand, response2.brand)
    }

    @Test
    fun `toEntity should handle all DeviceState values correctly`() {
        // Test all possible DeviceState values
        val states = listOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE)
        
        states.forEach { state ->
            // Given
            val request = CreateDeviceRequest(
                name = "Test Device",
                type = "test",
                brand = "Test Brand",
                state = state
            )

            // When
            val result = DeviceMapper.toEntity(request)

            // Then
            assertEquals(state, result.state, "Failed for state: $state")
        }
    }

    @Test
    fun `toResponse should handle different time zones correctly`() {
        // Given - temporarily clear mocking for this test
        unmockkAll()
        
        val utcTime = ZonedDateTime.parse("2025-06-25T10:00:00Z")
        val pstTime = ZonedDateTime.parse("2025-06-25T03:00:00-07:00")
        
        val device = Device(
            id = deviceId,
            name = "Timezone Test Device",
            type = "test",
            brand = "Test",
            state = DeviceState.AVAILABLE,
            creationTime = utcTime,
            updatedTime = pstTime
        )

        // When
        val result = DeviceMapper.toResponse(device)

        // Then
        assertEquals(utcTime.toString(), result.creationTime)
        assertEquals(pstTime.toString(), result.updatedTime)
        assertNotNull(result.id)
        assertEquals(device.name, result.name)
        
        // Restore mocking for other tests
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns fixedTime
    }
}
