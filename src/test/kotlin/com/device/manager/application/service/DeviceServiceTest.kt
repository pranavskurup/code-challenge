package com.device.manager.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.inbound.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class DeviceServiceTest {

    private val createDeviceUseCase = mockk<CreateDeviceUseCase>()
    private val updateDeviceUseCase = mockk<UpdateDeviceUseCase>()
    private val getDeviceUseCase = mockk<GetDeviceUseCase>()
    private val getAllDevicesUseCase = mockk<GetAllDevicesUseCase>()
    private val getDevicesByBrandUseCase = mockk<GetDevicesByBrandUseCase>()
    private val getDevicesByStateUseCase = mockk<GetDevicesByStateUseCase>()
    private val deleteDeviceUseCase = mockk<DeleteDeviceUseCase>()

    private val deviceService = DeviceService(
        createDeviceUseCase = createDeviceUseCase,
        updateDeviceUseCase = updateDeviceUseCase,
        getDeviceUseCase = getDeviceUseCase,
        getAllDevicesUseCase = getAllDevicesUseCase,
        getDevicesByBrandUseCase = getDevicesByBrandUseCase,
        getDevicesByStateUseCase = getDevicesByStateUseCase,
        deleteDeviceUseCase = deleteDeviceUseCase
    )

    private val fixedTime = ZonedDateTime.parse("2023-06-25T10:00:00Z")
    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute with CreateDeviceRequest should delegate to CreateDeviceUseCase and return success`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = request.name,
            type = request.type,
            brand = request.brand,
            state = request.state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        coEvery { createDeviceUseCase.execute(request) } returns Either.Right(expectedResponse)

        // When
        val result = deviceService.execute(request)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        coVerify { createDeviceUseCase.execute(request) }
    }

    @Test
    fun `execute with CreateDeviceRequest should delegate to CreateDeviceUseCase and return error`() = runTest {
        // Given
        val request = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val expectedError = DomainError.DeviceError.DeviceAlreadyExists("Device already exists")

        coEvery { createDeviceUseCase.execute(request) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(request)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { createDeviceUseCase.execute(request) }
    }

    @Test
    fun `execute with UpdateDeviceRequest should delegate to UpdateDeviceUseCase and return success`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15 Pro",
            type = null,
            brand = null,
            state = DeviceState.AVAILABLE
        )

        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15 Pro",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { updateDeviceUseCase.execute(deviceId, updateRequest) } returns Either.Right(expectedResponse)

        // When
        val result = deviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        coVerify { updateDeviceUseCase.execute(deviceId, updateRequest) }
    }

    @Test
    fun `execute with UpdateDeviceRequest should delegate to UpdateDeviceUseCase and return error`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15 Pro",
            type = null,
            brand = null,
            state = DeviceState.AVAILABLE
        )

        val expectedError = DomainError.DeviceError.DeviceNotFound("Device not found")

        coEvery { updateDeviceUseCase.execute(deviceId, updateRequest) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(deviceId, updateRequest)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { updateDeviceUseCase.execute(deviceId, updateRequest) }
    }

    @Test
    fun `execute with UUID should delegate to GetDeviceUseCase and return success`() = runTest {
        // Given
        val expectedResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        coEvery { getDeviceUseCase.execute(deviceId) } returns Either.Right(expectedResponse)

        // When
        val result = deviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(expectedResponse, response)

        coVerify { getDeviceUseCase.execute(deviceId) }
    }

    @Test
    fun `execute with UUID should delegate to GetDeviceUseCase and return error`() = runTest {
        // Given
        val expectedError = DomainError.DeviceError.DeviceNotFound("Device not found")

        coEvery { getDeviceUseCase.execute(deviceId) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { getDeviceUseCase.execute(deviceId) }
    }

    @Test
    fun `execute with no parameters should delegate to GetAllDevicesUseCase and return success`() = runTest {
        // Given
        val device1 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val device2 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "Galaxy S24",
            type = "smartphone",
            brand = "Samsung",
            state = DeviceState.IN_USE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val expectedResponses = listOf(device1, device2)

        coEvery { getAllDevicesUseCase.execute() } returns Either.Right(expectedResponses)

        // When
        val result = deviceService.execute()

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(2, responseList!!.size)
        assertEquals(expectedResponses, responseList)

        coVerify { getAllDevicesUseCase.execute() }
    }

    @Test
    fun `execute with no parameters should delegate to GetAllDevicesUseCase and return error`() = runTest {
        // Given
        val expectedError = DomainError.DeviceError("FETCH_ALL_ERROR", "Failed to fetch all devices")

        coEvery { getAllDevicesUseCase.execute() } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute()

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { getAllDevicesUseCase.execute() }
    }

    @Test
    fun `execute with brand String should delegate to GetDevicesByBrandUseCase and return success`() = runTest {
        // Given
        val brand = "Apple"
        val device1 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = brand,
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val device2 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "MacBook Pro",
            type = "laptop",
            brand = brand,
            state = DeviceState.IN_USE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val expectedResponses = listOf(device1, device2)

        coEvery { getDevicesByBrandUseCase.execute(brand) } returns Either.Right(expectedResponses)

        // When
        val result = deviceService.execute(brand)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(2, responseList!!.size)
        assertEquals(expectedResponses, responseList)
        assertTrue(responseList.all { it.brand == brand })

        coVerify { getDevicesByBrandUseCase.execute(brand) }
    }

    @Test
    fun `execute with brand String should delegate to GetDevicesByBrandUseCase and return error`() = runTest {
        // Given
        val brand = "NonExistentBrand"
        val expectedError = DomainError.DeviceError("BRAND_FETCH_ERROR", "Failed to fetch devices by brand")

        coEvery { getDevicesByBrandUseCase.execute(brand) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(brand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { getDevicesByBrandUseCase.execute(brand) }
    }

    @Test
    fun `execute with DeviceState should delegate to GetDevicesByStateUseCase and return success`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val device1 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val device2 = DeviceResponse(
            id = UUID.randomUUID(),
            name = "Galaxy S24",
            type = "smartphone",
            brand = "Samsung",
            state = state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val expectedResponses = listOf(device1, device2)

        coEvery { getDevicesByStateUseCase.execute(state) } returns Either.Right(expectedResponses)

        // When
        val result = deviceService.execute(state)

        // Then
        assertTrue(result.isRight())
        val responseList = result.getOrNull()
        assertNotNull(responseList)
        assertEquals(2, responseList!!.size)
        assertEquals(expectedResponses, responseList)
        assertTrue(responseList.all { it.state == state })

        coVerify { getDevicesByStateUseCase.execute(state) }
    }

    @Test
    fun `execute with DeviceState should delegate to GetDevicesByStateUseCase and return error`() = runTest {
        // Given
        val state = DeviceState.AVAILABLE
        val expectedError = DomainError.DeviceError("STATE_FETCH_ERROR", "Failed to fetch devices by state")

        coEvery { getDevicesByStateUseCase.execute(state) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(state)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { getDevicesByStateUseCase.execute(state) }
    }

    @Test
    fun `deleteDevice should delegate to DeleteDeviceUseCase and return success`() = runTest {
        // Given
        coEvery { deleteDeviceUseCase.execute(deviceId) } returns Either.Right(Unit)

        // When
        val result = deviceService.deleteDevice(deviceId)

        // Then
        assertTrue(result.isRight())
        val unitResult = result.getOrNull()
        assertNotNull(unitResult)
        assertEquals(Unit, unitResult)

        coVerify { deleteDeviceUseCase.execute(deviceId) }
    }

    @Test
    fun `deleteDevice should delegate to DeleteDeviceUseCase and return error`() = runTest {
        // Given
        val expectedError = DomainError.DeviceError.DeviceNotFound("Device not found")

        coEvery { deleteDeviceUseCase.execute(deviceId) } returns Either.Left(expectedError)

        // When
        val result = deviceService.deleteDevice(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { deleteDeviceUseCase.execute(deviceId) }
    }

    @Test
    fun `service should handle all DeviceState values correctly`() = runTest {
        // Test all possible DeviceState values
        val states = listOf(DeviceState.AVAILABLE, DeviceState.IN_USE, DeviceState.INACTIVE)

        states.forEach { state ->
            // Given
            clearMocks(getDevicesByStateUseCase)
            
            val device = DeviceResponse(
                id = UUID.randomUUID(),
                name = "Test Device for $state",
                type = "test",
                brand = "TestBrand",
                state = state,
                creationTime = fixedTime.toString(),
                updatedTime = fixedTime.toString()
            )

            val expectedResponses = listOf(device)

            coEvery { getDevicesByStateUseCase.execute(state) } returns Either.Right(expectedResponses)

            // When
            val result = deviceService.execute(state)

            // Then
            assertTrue(result.isRight(), "Failed for state: $state")
            val responseList = result.getOrNull()
            assertNotNull(responseList)
            assertEquals(1, responseList!!.size)
            assertEquals(state, responseList.first().state)

            coVerify { getDevicesByStateUseCase.execute(state) }
        }
    }

    @Test
    fun `service should handle empty brand correctly`() = runTest {
        // Given
        val emptyBrand = ""
        val expectedError = DomainError.DeviceError("INVALID_BRAND", "Brand cannot be empty")

        coEvery { getDevicesByBrandUseCase.execute(emptyBrand) } returns Either.Left(expectedError)

        // When
        val result = deviceService.execute(emptyBrand)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(expectedError, error)

        coVerify { getDevicesByBrandUseCase.execute(emptyBrand) }
    }

    @Test
    fun `service should handle concurrent operations correctly`() = runTest {
        // Given - multiple simultaneous operations
        val createRequest = CreateDeviceRequest(
            name = "iPhone 15",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE
        )

        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15 Pro",
            type = null,
            brand = null,
            state = null
        )

        val createResponse = DeviceResponse(
            id = deviceId,
            name = createRequest.name,
            type = createRequest.type,
            brand = createRequest.brand,
            state = createRequest.state,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.toString()
        )

        val updateResponse = DeviceResponse(
            id = deviceId,
            name = "iPhone 15 Pro",
            type = "smartphone",
            brand = "Apple",
            state = DeviceState.AVAILABLE,
            creationTime = fixedTime.toString(),
            updatedTime = fixedTime.plusHours(1).toString()
        )

        coEvery { createDeviceUseCase.execute(createRequest) } returns Either.Right(createResponse)
        coEvery { updateDeviceUseCase.execute(deviceId, updateRequest) } returns Either.Right(updateResponse)
        coEvery { getDeviceUseCase.execute(deviceId) } returns Either.Right(updateResponse)
        coEvery { deleteDeviceUseCase.execute(deviceId) } returns Either.Right(Unit)

        // When - execute operations
        val createResult = deviceService.execute(createRequest)
        val updateResult = deviceService.execute(deviceId, updateRequest)
        val getResult = deviceService.execute(deviceId)
        val deleteResult = deviceService.deleteDevice(deviceId)

        // Then - all operations should succeed
        assertTrue(createResult.isRight())
        assertTrue(updateResult.isRight())
        assertTrue(getResult.isRight())
        assertTrue(deleteResult.isRight())

        // Verify all use cases were called
        coVerify { createDeviceUseCase.execute(createRequest) }
        coVerify { updateDeviceUseCase.execute(deviceId, updateRequest) }
        coVerify { getDeviceUseCase.execute(deviceId) }
        coVerify { deleteDeviceUseCase.execute(deviceId) }
    }

    @Test
    fun `service should properly delegate to all use cases without side effects`() = runTest {
        // Given
        val brand = "Apple"
        val state = DeviceState.AVAILABLE
        val emptyList = emptyList<DeviceResponse>()

        coEvery { getAllDevicesUseCase.execute() } returns Either.Right(emptyList)
        coEvery { getDevicesByBrandUseCase.execute(brand) } returns Either.Right(emptyList)
        coEvery { getDevicesByStateUseCase.execute(state) } returns Either.Right(emptyList)

        // When
        val allDevicesResult = deviceService.execute()
        val brandResult = deviceService.execute(brand)
        val stateResult = deviceService.execute(state)

        // Then
        assertTrue(allDevicesResult.isRight())
        assertTrue(brandResult.isRight())
        assertTrue(stateResult.isRight())

        // Verify no cross-contamination between use cases
        coVerify(exactly = 1) { getAllDevicesUseCase.execute() }
        coVerify(exactly = 1) { getDevicesByBrandUseCase.execute(brand) }
        coVerify(exactly = 1) { getDevicesByStateUseCase.execute(state) }
        
        // Verify other use cases were not called
        coVerify(exactly = 0) { createDeviceUseCase.execute(any()) }
        coVerify(exactly = 0) { updateDeviceUseCase.execute(any(), any()) }
        coVerify(exactly = 0) { getDeviceUseCase.execute(any()) }
        coVerify(exactly = 0) { deleteDeviceUseCase.execute(any()) }
    }
}
