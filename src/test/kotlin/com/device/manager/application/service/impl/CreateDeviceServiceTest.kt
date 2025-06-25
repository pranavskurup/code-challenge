package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.dto.CreateDeviceRequest
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

class CreateDeviceServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val createDeviceService = CreateDeviceService(deviceRepository)

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")
    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        mockkObject(DeviceMapper)
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now() } returns fixedTime
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute should create device successfully when no duplicate exists`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val savedDevice = deviceEntity.copy(id = deviceId)
        
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Right(emptyList())
        coEvery { deviceRepository.save(deviceEntity) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
        coVerify { deviceRepository.save(deviceEntity) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }

    @Test
    fun `execute should return error when device with same name and brand already exists`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val existingDevice = Device(
            id = deviceId,
            name = request.name,
            type = "old-type",
            brand = request.brand,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusDays(1)
        )

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Right(listOf(existingDevice))

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertTrue(error is DomainError.DeviceError.DeviceAlreadyExists)
        assertEquals("Device with name 'iPhone 15' and brand 'Apple' already exists", error!!.message)

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
        coVerify(exactly = 0) { deviceRepository.save(any()) }
    }

    @Test
    fun `execute should create device when same brand exists but different name`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15 Pro",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 14", // Different name
            type = "smartphone",
            brand = request.brand,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusDays(1)
        )

        val savedDevice = deviceEntity.copy(id = UUID.randomUUID())
        
        val expectedResponse = DeviceResponse(
            id = savedDevice.id!!,
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Right(listOf(existingDevice))
        coEvery { deviceRepository.save(deviceEntity) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
        coVerify { deviceRepository.save(deviceEntity) }
        verify { DeviceMapper.toResponse(savedDevice) }
    }

    @Test
    fun `execute should return error when repository findByBrand fails`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val repositoryError = DomainError.DeviceError("REPOSITORY_ERROR", "Database connection failed")

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Left(repositoryError)

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(repositoryError, error)

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
        coVerify(exactly = 0) { deviceRepository.save(any()) }
    }

    @Test
    fun `execute should return error when repository save fails`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val saveError = DomainError.DeviceError("SAVE_ERROR", "Failed to save device")

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Right(emptyList())
        coEvery { deviceRepository.save(deviceEntity) } returns Either.Left(saveError)

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(saveError, error)

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
        coVerify { deviceRepository.save(deviceEntity) }
    }

    @Test
    fun `execute should handle exception and return generic error`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } throws RuntimeException("Unexpected error")

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_CREATION_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to create device"))
        assertTrue(error.message!!.contains("Unexpected error"))

        verify { DeviceMapper.toEntity(request) }
        coVerify { deviceRepository.findByBrand(request.brand) }
    }

    @Test
    fun `execute should handle case-sensitive name comparison`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iphone 15", // lowercase
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val deviceEntity = Device(
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val existingDevice = Device(
            id = deviceId,
            name = "iPhone 15", // uppercase
            type = "smartphone",
            brand = request.brand,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.minusDays(1),
            updatedTime = fixedTime.minusDays(1)
        )

        val savedDevice = deviceEntity.copy(id = UUID.randomUUID())
        val expectedResponse = DeviceResponse(
            id = savedDevice.id!!,
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        every { DeviceMapper.toEntity(request) } returns deviceEntity
        coEvery { deviceRepository.findByBrand(request.brand) } returns Either.Right(listOf(existingDevice))
        coEvery { deviceRepository.save(deviceEntity) } returns Either.Right(savedDevice)
        every { DeviceMapper.toResponse(savedDevice) } returns expectedResponse

        // When
        val result = createDeviceService.execute(request)

        // Then
        assertTrue(result.isRight()) // Should succeed because names are different (case-sensitive)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)
    }
}
