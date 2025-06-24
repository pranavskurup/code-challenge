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

class GetDevicesByBrandServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val getDevicesByBrandService = GetDevicesByBrandService(deviceRepository)

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")

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
    fun `execute should return devices when brand exists and has devices`() = runTest {
        // Given
        val brand = "Apple"
        val device1 = Device(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val device2 = Device(
            id = UUID.randomUUID(),
            name = "MacBook Pro",
            type = "laptop",
            brand = brand,
            state = DeviceState.IN_USE,
            creationTime = fixedTime.plusHours(1),
            updatedTime = fixedTime.plusHours(2)
        )

        val devices = listOf(device1, device2)

        val response1 = DeviceResponse(
            id = device1.id!!,
            name = device1.name,
            type = device1.type,
            brand = device1.brand,
            state = device1.state,
            creationTime = device1.creationTime.toString(),
            updatedTime = device1.updatedTime.toString()
        )

        val response2 = DeviceResponse(
            id = device2.id!!,
            name = device2.name,
            type = device2.type,
            brand = device2.brand,
            state = device2.state,
            creationTime = device2.creationTime.toString(),
            updatedTime = device2.updatedTime.toString()
        )

        val expectedResponses = listOf(response1, response2)

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(2, responseList!!.size)
        assertEquals(expectedResponses, responseList)
        assertTrue(responseList.all { it.brand == brand })

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should return empty list when brand exists but has no devices`() = runTest {
        // Given
        val brand = "Samsung"
        val emptyDeviceList = emptyList<Device>()
        val emptyResponseList = emptyList<DeviceResponse>()

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(emptyDeviceList)
        every { DeviceMapper.toResponseList(emptyDeviceList) } returns emptyResponseList

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertTrue(responseList!!.isEmpty())

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(emptyDeviceList) }
    }

    @Test
    fun `execute should return error when brand is empty string`() = runTest {
        // Given
        val emptyBrand = ""

        // When
        val result = getDevicesByBrandService.execute(emptyBrand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("INVALID_BRAND", error!!.code)
        assertEquals("Brand cannot be empty", error.message)

        coVerify(exactly = 0) { deviceRepository.findByBrand(any()) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should return error when brand is blank string`() = runTest {
        // Given
        val blankBrand = "   "

        // When
        val result = getDevicesByBrandService.execute(blankBrand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("INVALID_BRAND", error!!.code)
        assertEquals("Brand cannot be empty", error.message)

        coVerify(exactly = 0) { deviceRepository.findByBrand(any()) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should return error when repository fails`() = runTest {
        // Given
        val brand = "Apple"
        val repositoryError = DomainError.DeviceError("REPOSITORY_ERROR", "Database connection failed")

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Left(repositoryError)

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(repositoryError, error)

        coVerify { deviceRepository.findByBrand(brand) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle exception and return generic error`() = runTest {
        // Given
        val brand = "Apple"

        coEvery { deviceRepository.findByBrand(brand) } throws RuntimeException("Unexpected database error")

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_BY_BRAND_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices by brand"))
        assertTrue(error.message!!.contains("Unexpected database error"))

        coVerify { deviceRepository.findByBrand(brand) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle case-sensitive brand names`() = runTest {
        // Given
        val brand = "apple" // lowercase
        val device = Device(
            id = UUID.randomUUID(),
            name = "Test Device",
            type = "test",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)
        val expectedResponse = DeviceResponse(
            id = device.id!!,
            name = device.name,
            type = device.type,
            brand = brand,
            state = device.state,
            creationTime = device.creationTime.toString(),
            updatedTime = device.updatedTime.toString()
        )

        val expectedResponses = listOf(expectedResponse)

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(1, responseList!!.size)
        assertEquals(brand, responseList.first().brand)

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle devices with different states for same brand`() = runTest {
        // Given
        val brand = "Apple"
        val availableDevice = Device(
            id = UUID.randomUUID(),
            name = "Available iPhone",
            type = "smartphone",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val inUseDevice = Device(
            id = UUID.randomUUID(),
            name = "In Use MacBook",
            type = "laptop",
            brand = brand,
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val inactiveDevice = Device(
            id = UUID.randomUUID(),
            name = "Inactive iPad",
            type = "tablet",
            brand = brand,
            state = DeviceState.INACTIVE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(availableDevice, inUseDevice, inactiveDevice)

        val availableResponse = DeviceResponse(
            id = availableDevice.id!!,
            name = availableDevice.name,
            type = availableDevice.type,
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = availableDevice.creationTime.toString(),
            updatedTime = availableDevice.updatedTime.toString()
        )

        val inUseResponse = DeviceResponse(
            id = inUseDevice.id!!,
            name = inUseDevice.name,
            type = inUseDevice.type,
            brand = brand,
            state = DeviceState.IN_USE,
            creationTime = inUseDevice.creationTime.toString(),
            updatedTime = inUseDevice.updatedTime.toString()
        )

        val inactiveResponse = DeviceResponse(
            id = inactiveDevice.id!!,
            name = inactiveDevice.name,
            type = inactiveDevice.type,
            brand = brand,
            state = DeviceState.INACTIVE,
            creationTime = inactiveDevice.creationTime.toString(),
            updatedTime = inactiveDevice.updatedTime.toString()
        )

        val expectedResponses = listOf(availableResponse, inUseResponse, inactiveResponse)

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)

        val stateCount = responseList.groupBy { it.state }.mapValues { it.value.size }
        assertEquals(1, stateCount[DeviceState.AVAILABLE])
        assertEquals(1, stateCount[DeviceState.IN_USE])
        assertEquals(1, stateCount[DeviceState.INACTIVE])

        assertTrue(responseList.all { it.brand == brand })

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle special characters in brand name`() = runTest {
        // Given
        val brand = "Brands & Co."
        val device = Device(
            id = UUID.randomUUID(),
            name = "Special Brand Device",
            type = "test",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)
        val expectedResponse = DeviceResponse(
            id = device.id!!,
            name = device.name,
            type = device.type,
            brand = brand,
            state = device.state,
            creationTime = device.creationTime.toString(),
            updatedTime = device.updatedTime.toString()
        )

        val expectedResponses = listOf(expectedResponse)

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(1, responseList!!.size)
        assertEquals(brand, responseList.first().brand)

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle mapper throwing exception`() = runTest {
        // Given
        val brand = "Apple"
        val device = Device(
            id = UUID.randomUUID(),
            name = "Test Device",
            type = "test",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)

        coEvery { deviceRepository.findByBrand(brand) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } throws RuntimeException("Mapping error")

        // When
        val result = getDevicesByBrandService.execute(brand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_BY_BRAND_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices by brand"))
        assertTrue(error.message!!.contains("Mapping error"))

        coVerify { deviceRepository.findByBrand(brand) }
        verify { DeviceMapper.toResponseList(devices) }
    }
}
