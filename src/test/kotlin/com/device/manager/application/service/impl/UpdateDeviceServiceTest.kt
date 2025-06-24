package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.application.service.mapper.DeviceMapper
import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.outbound.DeviceRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class UpdateDeviceServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val updateDeviceService = UpdateDeviceService(deviceRepository)

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")
    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        mockkObject(DeviceMapper)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns fixedTime.plusHours(1)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute should update all fields when request contains all values`() = runTest {
        // Given
        val existingDevice = Device(
            id = deviceId,
            name = "Old iPhone",
            type = "old-smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusHours(2)
        )

        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15 Pro",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val updatedDevice = existingDevice.copy(
            name = updateRequest.name!!,
            type = updateRequest.type!!,
            brand = updateRequest.brand!!,
            state = updateRequest.state!!,
            updatedTime = fixedTime.plusHours(1)
        )

        val savedDevice = updatedDevice.copy()
        
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15 Pro",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.minusDays(1).toString(),
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
        coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        coVerify { deviceRepository.findById(deviceId) }
        coVerify { deviceRepository.update(deviceId, updatedDevice) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }

    @Test
    fun `execute should update only provided fields and keep others unchanged`() = runTest {
        // Given
        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 14",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusHours(2)
        )

        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15", // Only updating name
            type = null,
            brand = null,
            state = null
        )

        val updatedDevice = existingDevice.copy(
            name = "iPhone 15", // Updated
            // type, brand, state remain the same
            updatedTime = fixedTime.plusHours(1)
        )

        val savedDevice = updatedDevice.copy()
        
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15",
            type = "smartphone", // Unchanged
            brand = "Apple", // Unchanged
            state = DeviceState.INACTIVE, // Unchanged
            creationTime = fixedTime.minusDays(1).toString(),
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
        coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("iPhone 15", response!!.name)
        assertEquals("smartphone", response.type) // Should remain unchanged
        assertEquals("Apple", response.brand) // Should remain unchanged
        assertEquals(DeviceState.INACTIVE, response.state) // Should remain unchanged

        coVerify { deviceRepository.findById(deviceId) }
        coVerify { deviceRepository.update(deviceId, updatedDevice) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }

    @Test
    fun `execute should return error when device not found`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15",
            type = null,
            brand = null,
            state = null
        )

        val notFoundError = DomainError.DeviceError.DeviceNotFound("Device not found")

        coEvery { deviceRepository.findById(deviceId) } returns Either.Left(notFoundError)

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(notFoundError, error)

        coVerify { deviceRepository.findById(deviceId) }
        coVerify(exactly = 0) { deviceRepository.update(any(), any()) }
        verify(exactly = 0) { DeviceMapper.toResponse(any()) }
    }

    @Test
    fun `execute should return error when repository update fails`() = runTest {
        // Given
        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 14",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusHours(2)
        )

        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15",
            type = null,
            brand = null,
            state = DeviceState.AVAILABLE
        )

        val updatedDevice = existingDevice.copy(
            name = "iPhone 15",
            state = DeviceState.AVAILABLE,
            updatedTime = fixedTime.plusHours(1)
        )

        val updateError = DomainError.DeviceError("UPDATE_FAILED", "Failed to update device in database")

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
        coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Left(updateError)

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(updateError, error)

        coVerify { deviceRepository.findById(deviceId) }
        coVerify { deviceRepository.update(deviceId, updatedDevice) }
        verify(exactly = 0) { DeviceMapper.toResponse(any()) }
    }

    @Test
    fun `execute should handle exception and return generic error`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15",
            type = null,
            brand = null,
            state = null
        )

        coEvery { deviceRepository.findById(deviceId) } throws RuntimeException("Database connection error")

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_UPDATE_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to update device"))
        assertTrue(error.message!!.contains("Database connection error"))

        coVerify { deviceRepository.findById(deviceId) }
    }

    @Test
    fun `execute should update device state correctly`() = runTest {
        // Test updating to each possible state
        val states = listOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE)

        states.forEach { targetState ->
            // Given
            clearMocks(deviceRepository, DeviceMapper)
            
            val existingDevice = Device(
                id = deviceId,
                name = "Test Device",
                type = "test",
                brand = "Test Brand",
                state = DeviceState.AVAILABLE, // Start with AVAILABLE
                creationTime = fixedTime.minusDays(1),
                updatedTime = fixedTime.minusHours(2)
            )

            val updateRequest = UpdateDeviceRequest(
                name = null,
                type = null,
                brand = null,
                state = targetState
            )

            val updatedDevice = existingDevice.copy(
                state = targetState,
                updatedTime = fixedTime.plusHours(1)
            )

            val savedDevice = updatedDevice.copy()
            
            val expectedResponse = DeviceResponse(
                id = deviceId,
                name = "Test Device",
                type = "test",
                brand = "Test Brand",
                state = targetState,
                creationTime = fixedTime.minusDays(1).toString(),
                updatedTime = fixedTime.plusHours(1).toString()
            )

            coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
            coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Right(savedDevice)
            every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

            // When
            val result = updateDeviceService.execute(deviceId, updateRequest)

            // Then
            assertTrue(result.isRight(), "Failed for state: $targetState")
            val response = result.getOrNull()
            assertNotNull(response)
            assertEquals(targetState, response!!.state)
        }
    }

    @Test
    fun `execute should preserve creation time during update`() = runTest {
        // Given
        val originalCreationTime = fixedTime.minusDays(5)
        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 14",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = originalCreationTime,
            updatedTime = fixedTime.minusHours(2)
        )

        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15",
            type = "updated-smartphone",
            brand = "Apple Inc",
            state = DeviceState.AVAILABLE
        )

        val updatedDevice = existingDevice.copy(
            name = "iPhone 15",
            type = "updated-smartphone",
            brand = "Apple Inc",
            state = DeviceState.AVAILABLE,
            updatedTime = fixedTime.plusHours(1)
            // creationTime should remain unchanged
        )

        val savedDevice = updatedDevice.copy()
        
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15",
            type = "updated-smartphone",
            brand = "Apple Inc",
            state = DeviceState.AVAILABLE,
            creationTime = originalCreationTime.toString(), // Should be preserved
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
        coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(originalCreationTime.toString(), response!!.creationTime)
        assertEquals(fixedTime.plusHours(1).toString(), response.updatedTime)

        coVerify { deviceRepository.findById(deviceId) }
        coVerify { deviceRepository.update(deviceId, updatedDevice) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }

    @Test
    fun `execute should handle empty update request correctly`() = runTest {
        // Given
        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 14",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusHours(2)
        )

        val updateRequest = UpdateDeviceRequest(
            name = null,
            type = null,
            brand = null,
            state = null
        )

        val updatedDevice = existingDevice.copy(
            updatedTime = fixedTime.plusHours(1) // Only update time should change
        )

        val savedDevice = updatedDevice.copy()
        
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 14", // Unchanged
            type = "smartphone", // Unchanged
            brand = "Apple", // Unchanged
            state = DeviceState.INACTIVE, // Unchanged
            creationTime = fixedTime.minusDays(1).toString(),
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(existingDevice)
        coEvery { deviceRepository.update(deviceId, updatedDevice) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = updateDeviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        // All fields should remain the same except updated time
        assertEquals("iPhone 14", response!!.name)
        assertEquals("smartphone", response.type)
        assertEquals("Apple", response.brand)
        assertEquals(DeviceState.INACTIVE, response.state)
        assertEquals(fixedTime.plusHours(1).toString(), response.updatedTime)

        coVerify { deviceRepository.findById(deviceId) }
        coVerify { deviceRepository.update(deviceId, updatedDevice) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }
}
