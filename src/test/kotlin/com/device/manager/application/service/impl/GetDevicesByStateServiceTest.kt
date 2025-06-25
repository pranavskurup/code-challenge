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

class GetDevicesByStateServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val getDevicesByStateService = GetDevicesByStateService(deviceRepository)

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
    fun `execute should return devices when state AVAILABLE has devices`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val device1 = Device(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val device2 = Device(
            id = UUID.randomUUID(),
            name = "Galaxy S24",
            type = "smartphone",
            brand = "Samsung",
            state = state,
            creationTime = fixedTime.plusHours(1),
            updatedTime = fixedTime.plusHours(2)
        )

        val devices = listOf(device1, device2)

        val response1 = DeviceResponse(
            id = device1.id!!,
            name = device1.name,
            type = device1.type,
            brand = device1.brand,
            state = state,
            creationTime = device1.creationTime.toString(),
            updatedTime = device1.updatedTime.toString()
        )

        val response2 = DeviceResponse(
            id = device2.id!!,
            name = device2.name,
            type = device2.type,
            brand = device2.brand,
            state = state,
            creationTime = device2.creationTime.toString(),
            updatedTime = device2.updatedTime.toString()
        )

        val expectedResponses = listOf(response1, response2)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(2, responseList!!.size)
        assertEquals(expectedResponses, responseList)
        assertTrue(responseList.all { it.state == state })

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should return devices when state IN_USE has devices`() = runTest {
        // Given
        val state = DeviceState.IN_USE
        val device = Device(
            id = UUID.randomUUID(),
            name = "MacBook Pro",
            type = "laptop",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)

        val expectedResponse = DeviceResponse(
            id = device.id!!,
            name = device.name,
            type = device.type,
            brand = device.brand,
            state = state,
            creationTime = device.creationTime.toString(),
            updatedTime = device.updatedTime.toString()
        )

        val expectedResponses = listOf(expectedResponse)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(1, responseList!!.size)
        assertEquals(state, responseList.first().state)

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should return devices when state INACTIVE has devices`() = runTest {
        // Given
        val state = DeviceState.INACTIVE
        val device = Device(
            id = UUID.randomUUID(),
            name = "Old iPhone",
            type = "smartphone",
            brand = "Apple",
            state = state,
            creationTime = fixedTime.minusDays(30),
            updatedTime = fixedTime.minusDays(1)
        )

        val devices = listOf(device)

        val expectedResponse = DeviceResponse(
            id = device.id!!,
            name = device.name,
            type = device.type,
            brand = device.brand,
            state = state,
            creationTime = device.creationTime.toString(),
            updatedTime = device.updatedTime.toString()
        )

        val expectedResponses = listOf(expectedResponse)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(1, responseList!!.size)
        assertEquals(state, responseList.first().state)

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should return empty list when state has no devices`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val emptyDeviceList = emptyList<Device>()
        val emptyResponseList = emptyList<DeviceResponse>()

        coEvery { deviceRepository.findByState(state) } returns Either.Right(emptyDeviceList)
        every { DeviceMapper.toResponseList(emptyDeviceList) } returns emptyResponseList

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertTrue(responseList!!.isEmpty())

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(emptyDeviceList) }
    }

    @Test
    fun `execute should return error when repository fails`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val repositoryError = DomainError.DeviceError("REPOSITORY_ERROR", "Database connection failed")

        coEvery { deviceRepository.findByState(state) } returns Either.Left(repositoryError)

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(repositoryError, error)

        coVerify { deviceRepository.findByState(state) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle exception and return generic error`() = runTest {
        // Given
        val state = DeviceState.IN_USE

        coEvery { deviceRepository.findByState(state) } throws RuntimeException("Unexpected database error")

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_BY_STATE_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices by state"))
        assertTrue(error.message!!.contains("Unexpected database error"))

        coVerify { deviceRepository.findByState(state) }
        verify(exactly = 0) { DeviceMapper.toResponseList(any()) }
    }

    @Test
    fun `execute should handle all device states correctly`() = runTest {
        // Test all possible DeviceState values
        val states = listOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE)

        states.forEach { state ->
            // Given
            clearMocks(deviceRepository, DeviceMapper)
            
            val device = Device(
                id = UUID.randomUUID(),
                name = "Test Device for $state",
                type = "test",
                brand = "TestBrand",
                state = state,
                creationTime = fixedTime,
                updatedTime = fixedTime
            )

            val devices = listOf(device)
            val expectedResponse = DeviceResponse(
                id = device.id!!,
                name = device.name,
                type = device.type,
                brand = device.brand,
                state = state,
                creationTime = device.creationTime.toString(),
                updatedTime = device.updatedTime.toString()
            )

            val expectedResponses = listOf(expectedResponse)

            coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
            every { DeviceMapper.toResponseList(devices) } returns expectedResponses

            // When
            val result = getDevicesByStateService.execute(state)

            // Then
            assertTrue(result.isRight(), "Failed for state: $state")
            val responseList = result.getOrNull()
            assertNotNull(responseList)
            assertEquals(1, responseList!!.size)
            assertEquals(state, responseList.first().state)
        }
    }

    @Test
    fun `execute should handle devices with different brands for same state`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val appleDevice = Device(
            id = UUID.randomUUID(),
            name = "iPhone",
            type = "smartphone",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val samsungDevice = Device(
            id = UUID.randomUUID(),
            name = "Galaxy",
            type = "smartphone",
            brand = "Samsung",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val googleDevice = Device(
            id = UUID.randomUUID(),
            name = "Pixel",
            type = "smartphone",
            brand = "Google",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(appleDevice, samsungDevice, googleDevice)

        val appleResponse = DeviceResponse(
            id = appleDevice.id!!,
            name = appleDevice.name,
            type = appleDevice.type,
            brand = "Apple",
            state = state,
            creationTime = appleDevice.creationTime.toString(),
            updatedTime = appleDevice.updatedTime.toString()
        )

        val samsungResponse = DeviceResponse(
            id = samsungDevice.id!!,
            name = samsungDevice.name,
            type = samsungDevice.type,
            brand = "Samsung",
            state = state,
            creationTime = samsungDevice.creationTime.toString(),
            updatedTime = samsungDevice.updatedTime.toString()
        )

        val googleResponse = DeviceResponse(
            id = googleDevice.id!!,
            name = googleDevice.name,
            type = googleDevice.type,
            brand = "Google",
            state = state,
            creationTime = googleDevice.creationTime.toString(),
            updatedTime = googleDevice.updatedTime.toString()
        )

        val expectedResponses = listOf(appleResponse, samsungResponse, googleResponse)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)

        val brands = responseList.map { it.brand }.toSet()
        assertEquals(setOf("Apple", "Samsung", "Google"), brands)
        assertTrue(responseList.all { it.state == state })

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle large number of devices with same state`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val devices = (1..50).map { index ->
            Device(
                id = UUID.randomUUID(),
                name = "Device $index",
                type = "type$index",
                brand = "Brand${index % 5}",
                state = state,
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
                state = state,
                creationTime = device.creationTime.toString(),
                updatedTime = device.updatedTime.toString()
            )
        }

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(50, responseList!!.size)
        assertTrue(responseList.all { it.state == state })

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle mapper throwing exception`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val device = Device(
            id = UUID.randomUUID(),
            name = "Test Device",
            type = "test",
            brand = "TestBrand",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(device)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } throws RuntimeException("Mapping error")

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICES_BY_STATE_FETCH_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to fetch devices by state"))
        assertTrue(error.message!!.contains("Mapping error"))

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }

    @Test
    fun `execute should handle different device types for same state`() = runTest {
        // Given
        val state = DeviceState.IN_USE
        val smartphone = Device(
            id = UUID.randomUUID(),
            name = "iPhone",
            type = "smartphone",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val laptop = Device(
            id = UUID.randomUUID(),
            name = "MacBook",
            type = "laptop",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val tablet = Device(
            id = UUID.randomUUID(),
            name = "iPad",
            type = "tablet",
            brand = "Apple",
            state = state,
            creationTime = fixedTime,
            updatedTime = fixedTime
        )

        val devices = listOf(smartphone, laptop, tablet)

        val smartphoneResponse = DeviceResponse(
            id = smartphone.id!!,
            name = smartphone.name,
            type = "smartphone",
            brand = smartphone.brand,
            state = state,
            creationTime = smartphone.creationTime.toString(),
            updatedTime = smartphone.updatedTime.toString()
        )

        val laptopResponse = DeviceResponse(
            id = laptop.id!!,
            name = laptop.name,
            type = "laptop",
            brand = laptop.brand,
            state = state,
            creationTime = laptop.creationTime.toString(),
            updatedTime = laptop.updatedTime.toString()
        )

        val tabletResponse = DeviceResponse(
            id = tablet.id!!,
            name = tablet.name,
            type = "tablet",
            brand = tablet.brand,
            state = state,
            creationTime = tablet.creationTime.toString(),
            updatedTime = tablet.updatedTime.toString()
        )

        val expectedResponses = listOf(smartphoneResponse, laptopResponse, tabletResponse)

        coEvery { deviceRepository.findByState(state) } returns Either.Right(devices)
        every { DeviceMapper.toResponseList(devices) } returns expectedResponses

        // When
        val result = getDevicesByStateService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(3, responseList!!.size)

        val types = responseList.map { it.type }.toSet()
        assertEquals(setOf("smartphone", "laptop", "tablet"), types)
        assertTrue(responseList.all { it.state == state })

        coVerify { deviceRepository.findByState(state) }
        verify { DeviceMapper.toResponseList(devices) }
    }
}
