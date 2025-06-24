package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.dto.DeviceResponse
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

class GetDeviceServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val getDeviceService = GetDeviceService(deviceRepository)

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")
    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        mockkObject(DeviceMapper)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute should return device response when device exists`() = runTest {
        // Given
        val device = Device(
            id = deviceId,
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(device)
        every { DeviceMapper.toResponse(device) } returns expectedResponse

        // When
        val result = getDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        coVerify { deviceRepository.findById(deviceId) }
        verify { DeviceMapper.toResponse(device) }
    }

    @Test
    fun `execute should return error when device not found`() = runTest {
        // Given
        val notFoundError = DomainError.DeviceError.DeviceNotFound("Device not found")

        coEvery { deviceRepository.findById(deviceId) } returns Either.Left(notFoundError)

        // When
        val result = getDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(notFoundError, error)

        coVerify { deviceRepository.findById(deviceId) }
        verify(exactly = 0) { DeviceMapper.toResponse(any()) }
    }

    @Test
    fun `execute should return error when repository throws exception`() = runTest {
        // Given
        coEvery { deviceRepository.findById(deviceId) } throws RuntimeException("Database connection error")

        // When
        val result = getDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch device"))
        assertTrue(error.message!!.contains("Database connection error"))

        coVerify { deviceRepository.findById(deviceId) }
        verify(exactly = 0) { DeviceMapper.toResponse(any()) }
    }

    @Test
    fun `execute should handle device with different states`() = runTest {
        // Test all device states
        val states = listOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE)

        states.forEach { state ->
            // Given
            clearMocks(deviceRepository, DeviceMapper)
            
            val device = Device(
                id = deviceId,
                name = "Test Device",
                type = "test",
                brand = "Test Brand",
                state = state,
                creationTime = fixedTime,
                updatedTime = fixedTime
            )

            val expectedResponse = DeviceResponse(
                id = deviceId,
                name = "Test Device",
                type = "test",
                brand = "Test Brand",
                state = state,
                creationTime = fixedTime.toString(),
                updatedTime = fixedTime.toString()
            )

            coEvery { deviceRepository.findById(deviceId) } returns Either.Right(device)
            every { DeviceMapper.toResponse(device) } returns expectedResponse

            // When
            val result = getDeviceService.execute(deviceId)

            // Then
            assertTrue(result.isRight(), "Failed for state: $state")
            val response = result.getOrNull()
            assertNotNull(response)
            assertEquals(state, response!!.state)
        }
    }

    @Test
    fun `execute should handle device with null id in repository response`() = runTest {
        // Given
        val device = Device(
            id = null, // This shouldn't happen in practice but testing edge case
            name = "Test Device",
            type = "test",
            brand = "Test Brand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(device)
        every { DeviceMapper.toResponse(device) } throws IllegalArgumentException("Device ID cannot be null")

        // When
        val result = getDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Device ID cannot be null"))

        coVerify { deviceRepository.findById(deviceId) }
        verify { DeviceMapper.toResponse(device) }
    }

    @Test
    fun `execute should work with different UUID formats`() = runTest {
        // Given
        val differentDeviceId = UUID.randomUUID()
        val device = Device(
            id = differentDeviceId,
            name = "Another Device",
            type = "tablet",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime.plusDays(1),
            updatedTime = fixedTime.plusDays(2)
        )

        val expectedResponse = DeviceResponse(
            id = differentDeviceId,
            name = "Another Device",
            type = "tablet",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime.plusDays(1).toString(),
            updatedTime = fixedTime.plusDays(2).toString()
        )

        coEvery { deviceRepository.findById(differentDeviceId) } returns Either.Right(device)
        every { DeviceMapper.toResponse(device) } returns expectedResponse

        // When
        val result = getDeviceService.execute(differentDeviceId)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(differentDeviceId, response!!.id)
        assertEquals("Another Device", response.name)
        assertEquals(DeviceState.IN_USE, response.state)

        coVerify { deviceRepository.findById(differentDeviceId) }
        verify { DeviceMapper.toResponse(device) }
    }

    @Test
    fun `execute should handle repository returning different error types`() = runTest {
        val errorTypes = listOf(
            DomainError.DeviceError.DeviceNotFound("Custom not found message"),
            DomainError.DeviceError.DeviceUnauthorized("Unauthorized access"),
            DomainError.DeviceError("CUSTOM_ERROR", "Custom error message")
        )

        errorTypes.forEach { error ->
            // Given
            clearMocks(deviceRepository)
            coEvery { deviceRepository.findById(deviceId) } returns Either.Left(error)

            // When
            val result = getDeviceService.execute(deviceId)

            // Then
            assertTrue(result.isLeft(), "Failed for error: ${error.code}")
            val returnedError = result.leftOrNull()
            assertNotNull(returnedError)
            assertEquals(error, returnedError)
        }
    }

    @Test
    fun `execute should log device details on successful fetch`() = runTest {
        // Given
        val device = Device(
            id = deviceId,
            name = "Logging Test Device",
            type = "test",
            brand = "Test Brand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "Logging Test Device",
            type = "test",
            brand = "Test Brand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        coEvery { deviceRepository.findById(deviceId) } returns Either.Right(device)
        every { DeviceMapper.toResponse(device) } returns expectedResponse

        // When
        val result = getDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("Logging Test Device", response!!.name)
        assertEquals("Test Brand", response.brand)

        coVerify { deviceRepository.findById(deviceId) }
        verify { DeviceMapper.toResponse(device) }
    }
}
