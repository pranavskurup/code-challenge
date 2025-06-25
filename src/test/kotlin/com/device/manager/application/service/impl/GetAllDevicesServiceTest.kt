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

class GetAllDevicesServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val getAllDevicesService = GetAllDevicesService(deviceRepository)

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
    fun `execute should return empty list when no devices exist`() = runTest {
        // Given
        val emptyDeviceList = emptyList<Device>()
        val emptyResponseList = emptyList<DeviceResponse>()

        coEvery { deviceRepository.findAll() } returns Either.Right(emptyDeviceList)
        every { DeviceMapper.toResponseList(emptyDeviceList) } returns emptyResponseList

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertTrue(responseList!!.isEmpty())

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(emptyDeviceList) }
    }

    @Test
    fun `execute should return list of devices when devices exist`() = runTest {
        // Given
        val device1 = Device(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val device2 = Device(
            id = UUID.randomUUID(),
            name = "Galaxy S24",
            type = "smartphone",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime.plusHours(1),
            updatedTime = fixedTime.plusHours(2)
        )

        val device3 = Device(
            id = UUID.randomUUID(),
            name = "MacBook Pro",
            type = "laptop",
            brand = "Apple",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime.plusDays(1),
            updatedTime = fixedTime.plusDays(1)
        )

        val devices = listOf(device1, device2, device3)

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

        val response3 = DeviceResponse(
            id = device3.id!!,
            name = device3.name,
            type = device3.type,
            brand = device3.brand,
            state = device3.state,
            creationTime = device3.creationTime.toString(),
            updatedTime = device3.updatedTime.toString()
        )

        val expectedResponses = listOf(response1, response2, response3)

        coEvery { deviceRepository.findAll() } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)
        assertEquals(expectedResponses, responseList)

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should return error when repository fails`() = runTest {
        // Given
        val repositoryError = DomainError.DeviceError("REPOSITORY_ERROR", "Database connection failed")

        coEvery { deviceRepository.findAll() } returns Either.Left(repositoryError)

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(repositoryError, error)

        coVerify { deviceRepository.findAll() }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle exception and return generic error`() = runTest {
        // Given
        coEvery { deviceRepository.findAll() } throws RuntimeException("Unexpected database error")

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices"))
        assertTrue(error.message!!.contains("Unexpected database error"))

        coVerify { deviceRepository.findAll() }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle devices with different states`() = runTest {
        // Given
        val availableDevice = Device(
            id = UUID.randomUUID(),
            name = "Available Device",
            type = "test",
            brand = "TestBrand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val inUseDevice = Device(
            id = UUID.randomUUID(),
            name = "In Use Device",
            type = "test",
            brand = "TestBrand",
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val inactiveDevice = Device(
            id = UUID.randomUUID(),
            name = "Inactive Device",
            type = "test",
            brand = "TestBrand",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(availableDevice, inUseDevice, inactiveDevice)

        val availableResponse = DeviceResponse(
            id = availableDevice.id!!,
            name = availableDevice.name,
            type = availableDevice.type,
            brand = availableDevice.brand,
            state = DeviceState.AVAILABLE,
            creationTime = availableDevice.creationTime.toString(),
            updatedTime = availableDevice.updatedTime.toString()
        )

        val inUseResponse = DeviceResponse(
            id = inUseDevice.id!!,
            name = inUseDevice.name,
            type = inUseDevice.type,
            brand = inUseDevice.brand,
            state = DeviceState.IN_USE,
            creationTime = inUseDevice.creationTime.toString(),
            updatedTime = inUseDevice.updatedTime.toString()
        )

        val inactiveResponse = DeviceResponse(
            id = inactiveDevice.id!!,
            name = inactiveDevice.name,
            type = inactiveDevice.type,
            brand = inactiveDevice.brand,
            state = DeviceState.INACTIVE,
            creationTime = inactiveDevice.creationTime.toString(),
            updatedTime = inactiveDevice.updatedTime.toString()
        )

        val expectedResponses = listOf(availableResponse, inUseResponse, inactiveResponse)

        coEvery { deviceRepository.findAll() } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)

        val availableCount = responseList.count { it.state == DeviceState.AVAILABLE }
        val inUseCount = responseList.count { it.state == DeviceState.IN_USE }
        val inactiveCount = responseList.count { it.state == DeviceState.INACTIVE }

        assertEquals(1, availableCount)
        assertEquals(1, inUseCount)
        assertEquals(1, inactiveCount)

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle devices from different brands`() = runTest {
        // Given
        val appleDevice = Device(
            id = UUID.randomUUID(),
            name = "iPhone",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val samsungDevice = Device(
            id = UUID.randomUUID(),
            name = "Galaxy",
            type = "smartphone",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val googleDevice = Device(
            id = UUID.randomUUID(),
            name = "Pixel",
            type = "smartphone",
            brand = "Google",
            state = DeviceState.INACTIVE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(appleDevice, samsungDevice, googleDevice)

        val appleResponse = DeviceResponse(
            id = appleDevice.id!!,
            name = appleDevice.name,
            type = appleDevice.type,
            brand = "Apple",
            state = appleDevice.state,
            creationTime = appleDevice.creationTime.toString(),
            updatedTime = appleDevice.updatedTime.toString()
        )

        val samsungResponse = DeviceResponse(
            id = samsungDevice.id!!,
            name = samsungDevice.name,
            type = samsungDevice.type,
            brand = "Samsung",
            state = samsungDevice.state,
            creationTime = samsungDevice.creationTime.toString(),
            updatedTime = samsungDevice.updatedTime.toString()
        )

        val googleResponse = DeviceResponse(
            id = googleDevice.id!!,
            name = googleDevice.name,
            type = googleDevice.type,
            brand = "Google",
            state = googleDevice.state,
            creationTime = googleDevice.creationTime.toString(),
            updatedTime = googleDevice.updatedTime.toString()
        )

        val expectedResponses = listOf(appleResponse, samsungResponse, googleResponse)

        coEvery { deviceRepository.findAll() } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)

        val brands = responseList.map { it.brand }.toSet()
        assertEquals(setOf("Apple", "Samsung", "Google"), brands)

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle large number of devices`() = runTest {
        // Given
        val devices = (1..100).map { index ->
            Device(
                id = UUID.randomUUID(),
                name = "Device $index",
                type = "type$index",
                brand = "Brand${index % 5}", // 5 different brands
                state = DeviceState.values()[index % 3], // Cycle through states
                creationTime = fixedTime.plusMinutes(index.toLong()),
                updatedTime = fixedTime.plusMinutes(index.toLong())
            )
        }

        val expectedResponses = devices.map { device ->
            DeviceResponse(
                id = device.id!!,
                name = device.name,
                type = device.type,
                brand = device.brand,
                state = device.state,
                creationTime = device.creationTime.toString(),
                updatedTime = device.updatedTime.toString()
            )
        }

        coEvery { deviceRepository.findAll() } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(100, responseList!!.size)
        assertEquals(expectedResponses, responseList)

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle mapper throwing exception`() = runTest {
        // Given
        val device = Device(
            id = UUID.randomUUID(),
            name = "Test Device",
            type = "test",
            brand = "TestBrand",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)

        coEvery { deviceRepository.findAll() } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } throws RuntimeException("Mapping error")

        // When
        val result = getAllDevicesService.execute()

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices"))
        assertTrue(error.message!!.contains("Mapping error"))

        coVerify { deviceRepository.findAll() }
        verify { DeviceMapper.toResponseList(devices) }
    }
}
