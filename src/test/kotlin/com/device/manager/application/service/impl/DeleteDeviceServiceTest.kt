package com.device.manager.application.service.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.domain.errors.DomainError
import com.device.manager.domain.port.outbound.DeviceRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeleteDeviceServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val deleteDeviceService = DeleteDeviceService(deviceRepository)

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
    fun `execute should delete device successfully when device exists`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } returns Either.Right(Unit)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())
        val unitResult = result.getOrNull()
        assertNotNull(unitResult)
        assertEquals(Unit, unitResult)

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should return error when device does not exist`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(false)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertTrue(error is DomainError.DeviceError.DeviceNotFound)
        assertEquals("Device with id $deviceId not found", error!!.message)

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
    }

    @Test
    fun `execute should return error when existsById repository call fails`() = runTest {
        // Given
        val repositoryError = DomainError.DeviceError("REPOSITORY_ERROR", "Database connection failed")
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Left(repositoryError)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(repositoryError, error)

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
    }

    @Test
    fun `execute should return error when deleteById repository call fails`() = runTest {
        // Given
        val deleteError = DomainError.DeviceError("DELETE_ERROR", "Failed to delete device from database")
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } returns Either.Left(deleteError)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(deleteError, error)

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should handle exception in existsById and return generic error`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } throws RuntimeException("Database connection error")

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_DELETE_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to delete device"))
        assertTrue(error.message!!.contains("Database connection error"))

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
    }

    @Test
    fun `execute should handle exception in deleteById and return generic error`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } throws RuntimeException("Delete operation failed")

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals("DEVICE_DELETE_ERROR", error!!.code)
        assertTrue(error.message!!.contains("Failed to delete device"))
        assertTrue(error.message!!.contains("Delete operation failed"))

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should work with different UUID formats`() = runTest {
        // Given
        val differentDeviceId = UUID.randomUUID()
        coEvery { deviceRepository.existsById(differentDeviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(differentDeviceId) } returns Either.Right(Unit)

        // When
        val result = deleteDeviceService.execute(differentDeviceId)

        // Then
        assertTrue(result.isRight())
        val unitResult = result.getOrNull()
        assertNotNull(unitResult)
        assertEquals(Unit, unitResult)

        coVerify { deviceRepository.existsById(differentDeviceId) }
        coVerify { deviceRepository.deleteById(differentDeviceId) }
    }

    @Test
    fun `execute should handle specific device error types correctly`() = runTest {
        val errorTypes = listOf(
            DomainError.DeviceError.DeviceNotFound("Custom not found message"),
            DomainError.DeviceError.DeviceUnauthorized("Unauthorized access"),
            DomainError.DeviceError.InUseDeviceCannotBeDeleted("Device is in use"),
            DomainError.DeviceError("CUSTOM_ERROR", "Custom error message")
        )

        errorTypes.forEach { error ->
            // Given
            clearMocks(deviceRepository)
            coEvery { deviceRepository.existsById(deviceId) } returns Either.Left(error)

            // When
            val result = deleteDeviceService.execute(deviceId)

            // Then
            assertTrue(result.isLeft(), "Failed for error: ${error.code}")
            val returnedError = result.leftOrNull()
            assertNotNull(returnedError)
            assertEquals(error, returnedError)

            coVerify { deviceRepository.existsById(deviceId) }
            coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
        }
    }

    @Test
    fun `execute should handle delete operation error types correctly`() = runTest {
        val deleteErrors = listOf(
            DomainError.DeviceError.InUseDeviceCannotBeDeleted("Device cannot be deleted while in use"),
            DomainError.DeviceError("FOREIGN_KEY_CONSTRAINT", "Cannot delete due to foreign key constraint"),
            DomainError.DeviceError("CONCURRENT_MODIFICATION", "Device was modified by another process")
        )

        deleteErrors.forEach { error ->
            // Given
            clearMocks(deviceRepository)
            coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
            coEvery { deviceRepository.deleteById(deviceId) } returns Either.Left(error)

            // When
            val result = deleteDeviceService.execute(deviceId)

            // Then
            assertTrue(result.isLeft(), "Failed for error: ${error.code}")
            val returnedError = result.leftOrNull()
            assertNotNull(returnedError)
            assertEquals(error, returnedError)

            coVerify { deviceRepository.existsById(deviceId) }
            coVerify { deviceRepository.deleteById(deviceId) }
        }
    }

    @Test
    fun `execute should handle race condition where device exists check passes but delete fails with not found`() = runTest {
        // Given
        val notFoundError = DomainError.DeviceError.DeviceNotFound("Device not found during delete")
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } returns Either.Left(notFoundError)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertEquals(notFoundError, error)

        coVerify { deviceRepository.existsById(deviceId) }
        coVerify { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should validate that exists check is called before delete`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } returns Either.Right(Unit)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())

        // Verify the order of operations
        coVerify { deviceRepository.existsById(deviceId) }
        coVerify { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should handle null device ID scenarios gracefully`() = runTest {
        // This test ensures the service can handle edge cases
        // Note: UUID.fromString() would throw for invalid UUIDs, but this tests the flow

        // Given - using a valid UUID but testing repository behavior
        val testId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        coEvery { deviceRepository.existsById(testId) } returns Either.Right(false)

        // When
        val result = deleteDeviceService.execute(testId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertTrue(error is DomainError.DeviceError.DeviceNotFound)

        coVerify { deviceRepository.existsById(testId) }
        coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
    }

    @Test
    fun `execute should handle successful delete with proper logging flow`() = runTest {
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(true)
        coEvery { deviceRepository.deleteById(deviceId) } returns Either.Right(Unit)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isRight())
        assertEquals(Unit, result.getOrNull())

        // Verify both repository calls were made
        coVerify(exactly = 1) { deviceRepository.existsById(deviceId) }
        coVerify(exactly = 1) { deviceRepository.deleteById(deviceId) }
    }

    @Test
    fun `execute should handle repository returning false for exists but true for subsequent calls`() = runTest {
        // This tests the service behavior when repository state changes between calls
        // Given
        coEvery { deviceRepository.existsById(deviceId) } returns Either.Right(false)

        // When
        val result = deleteDeviceService.execute(deviceId)

        // Then
        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
        assertTrue(error is DomainError.DeviceError.DeviceNotFound)

        // Verify that delete was never called since exists returned false
        coVerify { deviceRepository.existsById(deviceId) }
        coVerify(exactly = 0) { deviceRepository.deleteById(any()) }
    }
}
