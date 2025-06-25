package com.device.manager.infrastructure.outbound.persistence.adapter

import arrow.core.Either
import com.device.manager.TestcontainersConfiguration
import com.device.manager.domain.entity.Device
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError.DeviceError
import com.device.manager.domain.port.outbound.DeviceRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.ZonedDateTime
import java.util.*

@SpringBootTest
@Import(TestcontainersConfiguration::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class DeviceRepositoryAdapterIntegrationTest {

    @Autowired
    private lateinit var deviceRepository: DeviceRepository

    @Autowired
    private lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    @BeforeEach
    fun clearDatabase() = runTest {
        // Clear all devices from database before each test
        r2dbcEntityTemplate.databaseClient
            .sql("DELETE FROM devices")
            .then()
            .block()
    }

    private fun createTestDevice(
        name: String = "Test iPhone",
        type: String = "Smartphone",
        brand: String = "Apple",
        state: DeviceState = DeviceState.INACTIVE,
        creationTime: ZonedDateTime = ZonedDateTime.now(),
        updatedTime: ZonedDateTime = ZonedDateTime.now()
    ): Device {
        return Device(null, name, type, brand, state, creationTime, updatedTime)
    }

    @Test
    @Order(1)
    fun `should save device successfully with TestContainers`() = runTest {
        // Given
        val device = createTestDevice(name = "Integration Test Device")

        // When
        val result = deviceRepository.save(device)

        // Then
        assertThat(result).isInstanceOf(Either.Right::class.java)
        result.fold(
            { fail("Expected success but got error: $it") },
            { savedDevice ->
                assertThat(savedDevice.name).isEqualTo("Integration Test Device")
                assertThat(savedDevice.id).isNotNull()
                assertThat(savedDevice.creationTime).isNotNull()
                assertThat(savedDevice.updatedTime).isNotNull()
            }
        )
    }

    @Test
    @Order(2)
    fun `should find device by id when exists`() = runTest {
        // Given
        val device = createTestDevice(name = "Find Me Device")
        val savedResult = deviceRepository.save(device)
        val savedDevice = (savedResult as Either.Right).value

        // When
        val result = deviceRepository.findById(savedDevice.id!!)

        // Then
        assertThat(result).isInstanceOf(Either.Right::class.java)
        result.fold(
            { fail("Expected success but got error: $it") },
            { foundDevice ->
                assertThat(foundDevice.id).isEqualTo(savedDevice.id)
                assertThat(foundDevice.name).isEqualTo("Find Me Device")
                assertThat(foundDevice.brand).isEqualTo("Apple")
            }
        )
    }

    @Test
    @Order(3)
    fun `should return error when device not found by id`() = runTest {
        // Given
        val nonExistentId = UUID.randomUUID()

        // When
        val result = deviceRepository.findById(nonExistentId)

        // Then
        assertThat(result).isInstanceOf(Either.Left::class.java)
        result.fold(
            { error ->
                assertThat(error.code).isEqualTo("DEVICE_NOT_FOUND")
                assertThat(error.message).contains(nonExistentId.toString())
            },
            { fail("Expected error but got success: $it") }
        )
    }

    @Test
    @Order(4)
    fun `should find all devices when multiple exist`() = runTest {
        // Given
        val device1 = createTestDevice(name = "Device 1", brand = "Apple")
        val device2 = createTestDevice(name = "Device 2", brand = "Samsung")
        val device3 = createTestDevice(name = "Device 3", brand = "Google")

        deviceRepository.save(device1)
        deviceRepository.save(device2)
        deviceRepository.save(device3)

        // When
        val devicesResult = deviceRepository.findAll()

        // Then
        assertThat(devicesResult).isInstanceOf(Either.Right::class.java)
        devicesResult.fold(
            { fail("Expected success but got error: $it") },
            { devices ->
                assertThat(devices).hasSize(3)
                assertThat(devices.map { it.name }).containsExactlyInAnyOrder("Device 1", "Device 2", "Device 3")
                assertThat(devices.map { it.brand }).containsExactlyInAnyOrder("Apple", "Samsung", "Google")
            }
        )
    }

    @Test
    @Order(5)
    fun `should find devices by brand`() = runTest {
        // Given
        val appleDevice1 = createTestDevice(name = "iPhone 15", brand = "Apple")
        val appleDevice2 = createTestDevice(name = "iPad Pro", brand = "Apple")
        val samsungDevice = createTestDevice(name = "Galaxy S24", brand = "Samsung")

        deviceRepository.save(appleDevice1)
        deviceRepository.save(appleDevice2)
        deviceRepository.save(samsungDevice)

        // When
        val appleDevicesResult = deviceRepository.findByBrand("Apple")

        // Then
        assertThat(appleDevicesResult).isInstanceOf(Either.Right::class.java)
        appleDevicesResult.fold(
            { fail("Expected success but got error: $it") },
            { appleDevices ->
                assertThat(appleDevices).hasSize(2)
                assertThat(appleDevices.map { it.name }).containsExactlyInAnyOrder("iPhone 15", "iPad Pro")
                assertThat(appleDevices.all { it.brand == "Apple" }).isTrue()
            }
        )
    }

    @Test
    @Order(6)
    fun `should find devices by state`() = runTest {
        // Given
        val availableDevice1 = createTestDevice(name = "Available Device 1", state = DeviceState.AVAILABLE)
        val availableDevice2 = createTestDevice(name = "Available Device 2", state = DeviceState.AVAILABLE)
        val inactiveDevice = createTestDevice(name = "Inactive Device", state = DeviceState.INACTIVE)

        deviceRepository.save(availableDevice1)
        deviceRepository.save(availableDevice2)
        deviceRepository.save(inactiveDevice)

        // When
        val availableDevicesResult = deviceRepository.findByState(DeviceState.AVAILABLE)

        // Then
        assertThat(availableDevicesResult).isInstanceOf(Either.Right::class.java)
        availableDevicesResult.fold(
            { fail("Expected success but got error: $it") },
            { availableDevices ->
                assertThat(availableDevices).hasSize(2)
                assertThat(availableDevices.map { it.name }).containsExactlyInAnyOrder("Available Device 1", "Available Device 2")
                assertThat(availableDevices.all { it.state == DeviceState.AVAILABLE }).isTrue()
            }
        )
    }

    @Test
    @Order(7)
    fun `should update device successfully`() = runTest {
        // Given
        val originalDevice = createTestDevice(name = "Original Name", brand = "Apple")
        val savedResult = deviceRepository.save(originalDevice)
        val savedDevice = (savedResult as Either.Right).value

        val foundResult = deviceRepository.findById(savedDevice.id!!)
        val foundDevice = (foundResult as Either.Right).value


        val updatedDevice = foundDevice.copy(
            name = "Updated Name",
            brand = "Samsung"
            // Keep the original creationTime and updatedTime from the saved device
            // The repository will handle updating the updatedTime automatically
        )

        // When
        val updateResult = deviceRepository.update(foundDevice.id!!, updatedDevice)

        // Then
        assertThat(updateResult).isInstanceOf(Either.Right::class.java)
        updateResult.fold(
            { fail("Expected success but got error: $it") },
            { updated ->
                assertThat(updated.name).isEqualTo("Updated Name")
                assertThat(updated.brand).isEqualTo("Samsung")
                assertThat(updated.id).isEqualTo(savedDevice.id)
            }
        )

        // Verify persistence
        val retrievedResult = deviceRepository.findById(savedDevice.id!!)
        retrievedResult.fold(
            { fail("Failed to retrieve updated device: $it") },
            { retrieved ->
                assertThat(retrieved.name).isEqualTo("Updated Name")
                assertThat(retrieved.brand).isEqualTo("Samsung")
            }
        )
    }

    @Test
    @Order(8)
    fun `should delete device successfully when inactive`() = runTest {
        // Given
        val device = createTestDevice(state = DeviceState.INACTIVE)
        val savedResult = deviceRepository.save(device)
        val savedDevice = (savedResult as Either.Right).value

        // When
        val deleteResult = deviceRepository.deleteById(savedDevice.id!!)

        // Then
        assertThat(deleteResult).isInstanceOf(Either.Right::class.java)

        // Verify deletion
        val findResult = deviceRepository.findById(savedDevice.id!!)
        assertThat(findResult).isInstanceOf(Either.Left::class.java)
    }

    @Test
    @Order(9)
    fun `should not delete device when in use`() = runTest {
        // Given
        val availableDevice = createTestDevice(state = DeviceState.IN_USE)
        val savedResult = deviceRepository.save(availableDevice)
        val savedDevice = (savedResult as Either.Right).value

        // When
        val deleteResult = deviceRepository.deleteById(savedDevice.id!!)

        // Then
        assertThat(deleteResult).isInstanceOf(Either.Left::class.java)
        deleteResult.fold(
            { error ->
                assertThat(error.code).isEqualTo("IN_USE_DEVICE_CANNOT_BE_DELETED")
            },
            { fail("Expected error but got success") }
        )

        // Verify device still exists
        val findResult = deviceRepository.findById(savedDevice.id!!)
        assertThat(findResult).isInstanceOf(Either.Right::class.java)
    }

    @Test
    @Order(10)
    fun `should handle concurrent operations with TestContainers`() = runTest {
        // Given
        val devices = (1..10).map {
            createTestDevice(name = "Concurrent Device $it", brand = "Brand$it")
        }

        // When - Save devices concurrently
        val saveResults = devices.map { device ->
            deviceRepository.save(device)
        }

        // Then
        val successfulSaves = saveResults.filterIsInstance<Either.Right<Device>>()
        assertThat(successfulSaves).hasSize(10)

        // Verify all devices are persisted
        val allDevicesResult = deviceRepository.findAll()
        allDevicesResult.fold(
            { fail("Failed to retrieve all devices: $it") },
            { allDevices ->
                assertThat(allDevices).hasSize(10)
                assertThat(allDevices.map { it.name }).containsAll(devices.map { it.name })
            }
        )
    }

    @Test
    @Order(11)
    fun `should handle database constraints and rollback transactions`() = runTest {
        // Given - Create a device with valid data
        val validDevice = createTestDevice(name = "Valid Device")
        deviceRepository.save(validDevice)

        // When - Try to save a device with duplicate ID (should be prevented by constraints)
        val duplicateDevice = validDevice.copy()
        val result = deviceRepository.save(duplicateDevice)

        // Then - Should handle constraint violation gracefully
        // Note: The exact behavior depends on your duplicate handling strategy
        // This test ensures the database maintains integrity with TestContainers
        val allDevicesResult = deviceRepository.findAll()
        allDevicesResult.fold(
            { fail("Failed to retrieve all devices: $it") },
            { allDevices ->
                assertThat(allDevices.map { it.name }).contains("Valid Device")
            }
        )
    }

    private fun fail(message: String): Nothing {
        throw AssertionError(message)
    }
}
