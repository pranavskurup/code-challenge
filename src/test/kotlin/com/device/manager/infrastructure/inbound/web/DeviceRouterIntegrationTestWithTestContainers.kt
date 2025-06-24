package com.device.manager.infrastructure.inbound.web

import com.device.manager.TestcontainersConfiguration
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfiguration::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class DeviceRouterIntegrationTestWithTestContainers {

    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Autowired
    private lateinit var r2dbcEntityTemplate: R2dbcEntityTemplate

    @BeforeAll
    fun setUp() {
        // TestContainers are automatically managed by Spring Boot TestContainers support
        println("TestContainers integration test setup completed")
    }
    
    @BeforeEach
    fun clearDatabase() = runTest {
        // Clear all devices from database before each test
        r2dbcEntityTemplate.databaseClient
            .sql("DELETE FROM devices")
            .then()
            .block()
        println("Database cleared before test")
    }

    @AfterAll
    fun tearDown() {
        // TestContainers are automatically cleaned up
        println("TestContainers integration test cleanup completed")
    }

    // Helper method to create a device for testing
    private suspend fun createTestDevice(
        name: String = "Test iPhone",
        type: String = "Smartphone", 
        brand: String = "Apple",
        state: DeviceState = DeviceState.INACTIVE
    ): String {
        val createRequest = CreateDeviceRequest(name, type, brand, state)
        
        val responseBody = webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!
            
        return extractDeviceIdFromResponse(responseBody) 
            ?: throw IllegalStateException("Failed to extract device ID from response")
    }

    @Test
    @Order(1)
    fun `should create device successfully with database persistence`() = runTest {
        val createRequest = CreateDeviceRequest(
            name = "Test iPhone",
            type = "Smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE
        )

        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Device created successfully")
            .jsonPath("$.data.name").isEqualTo("Test iPhone")
            .jsonPath("$.data.type").isEqualTo("Smartphone")
            .jsonPath("$.data.brand").isEqualTo("Apple")
            .jsonPath("$.data.state").isEqualTo("INACTIVE")
            .jsonPath("$.data.id").exists()

        // Verify the response structure with a second device
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest.copy(name = "Test iPhone 2"))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Device created successfully")
            .jsonPath("$.data.name").isEqualTo("Test iPhone 2")
            .jsonPath("$.data.type").isEqualTo("Smartphone")
            .jsonPath("$.data.brand").isEqualTo("Apple")
            .jsonPath("$.data.state").isEqualTo("INACTIVE")
            .jsonPath("$.data.id").exists()
    }

    @Test
    @Order(2)
    fun `should get device by id from database`() = runTest {
        val deviceId = createTestDevice()

        webTestClient.get()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(deviceId)
            .jsonPath("$.data.name").isEqualTo("Test iPhone")
            .jsonPath("$.data.brand").isEqualTo("Apple")
    }

    @Test
    @Order(3)
    fun `should get all devices from database`() = runTest {
        createTestDevice()
        
        webTestClient.get()
            .uri("/api/v1/devices")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isNumber
    }

    @Test
    @Order(4)
    fun `should get devices by brand from database`() = runTest {
        createTestDevice(brand = "Apple")
        
        webTestClient.get()
            .uri("/api/v1/devices/brand/Apple")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data[?(@.brand == 'Apple')]").exists()
    }

    @Test
    @Order(5)
    fun `should get devices by state from database`() = runTest {
        createTestDevice(state = DeviceState.INACTIVE)
        
        webTestClient.get()
            .uri("/api/v1/devices/state/inactive")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data[?(@.state == 'INACTIVE')]").exists()
    }

    @Test
    @Order(6)
    fun `should update device in database`() = runTest {
        val deviceId = createTestDevice()

        val updateRequest = UpdateDeviceRequest(
            name = "Updated Test iPhone",
            type = "Updated Smartphone",
            brand = null, // Keep existing brand
            state = DeviceState.AVAILABLE
        )

        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Device updated successfully")
            .jsonPath("$.data.name").isEqualTo("Updated Test iPhone")
            .jsonPath("$.data.type").isEqualTo("Updated Smartphone")
            .jsonPath("$.data.brand").isEqualTo("Apple") // Should remain unchanged
            .jsonPath("$.data.state").isEqualTo("AVAILABLE")
    }

    @Test
    @Order(7)
    fun `should verify updated device persisted in database`() = runTest {
        val deviceId = createTestDevice()

        // Update the device
        val updateRequest = UpdateDeviceRequest(
            name = "Updated Test iPhone",
            type = "Updated Smartphone",
            brand = null, // Keep existing brand
            state = DeviceState.AVAILABLE
        )

        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk

        // Verify the update persisted
        webTestClient.get()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.name").isEqualTo("Updated Test iPhone")
            .jsonPath("$.data.state").isEqualTo("AVAILABLE")
    }

    @Test
    @Order(8)
    fun `should handle creation time immutability validation`() = runTest {
        // Create a device that is in use first
        val inUseDeviceRequest = CreateDeviceRequest(
            name = "In Use Device",
            type = "Tablet",
            brand = "Samsung",
            state = DeviceState.IN_USE
        )

        val createResult = webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(inUseDeviceRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()

        val inUseDeviceId = extractDeviceIdFromResponse(createResult.responseBody!!)

        // Try to update name and brand of in-use device
        val updateRequest = UpdateDeviceRequest(
            name = "New Name",
            type = null,
            brand = "New Brand",
            state = null
        )

        webTestClient.put()
            .uri("/api/v1/devices/{id}", inUseDeviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("IN_USE_DEVICE_IMMUTABLE_FIELDS")
    }

    @Test
    @Order(9)
    fun `should prevent deleting device that is in use`() = runTest {
        // Create a device in use
        val inUseDeviceRequest = CreateDeviceRequest(
            name = "Delete Test Device",
            type = "Laptop",
            brand = "Dell",
            state = DeviceState.IN_USE
        )

        val createResult = webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(inUseDeviceRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()

        val inUseDeviceId = extractDeviceIdFromResponse(createResult.responseBody!!)

        // Try to delete the in-use device
        webTestClient.delete()
            .uri("/api/v1/devices/{id}", inUseDeviceId)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("IN_USE_DEVICE_CANNOT_BE_DELETED")
    }

    @Test
    @Order(10)
    fun `should handle duplicate device creation`() = runTest {
        // First, create a device
        createTestDevice(name = "Test iPhone", brand = "Apple")
        
        // Then try to create the same device again (duplicate)
        val duplicateRequest = CreateDeviceRequest(
            name = "Test iPhone", // Same name as created device
            type = "Smartphone",
            brand = "Apple", // Same brand as created device
            state = DeviceState.INACTIVE
        )

        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(duplicateRequest)
            .exchange()
            .expectStatus().isEqualTo(409) // Conflict
            .expectBody()
            .jsonPath("$.code").isEqualTo("DEVICE_ALREADY_EXISTS")
    }

    @Test
    @Order(11)
    fun `should handle invalid UUID format`() = runTest {
        webTestClient.get()
            .uri("/api/v1/devices/invalid-uuid")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_UUID")
    }

    @Test
    @Order(12)
    fun `should handle non-existent device`() = runTest {
        val nonExistentId = UUID.randomUUID()

        webTestClient.get()
            .uri("/api/v1/devices/{id}", nonExistentId)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.code").isEqualTo("DEVICE_NOT_FOUND")
    }

    @Test
    @Order(13)
    fun `should handle invalid device state`() = runTest {
        webTestClient.get()
            .uri("/api/v1/devices/state/invalid-state")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_STATE")
    }

    @Test
    @Order(14)
    fun `should handle empty brand parameter`() = runTest {
        webTestClient.get()
            .uri("/api/v1/devices/brand/ ")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_BRAND")
    }

    @Test
    @Order(15)
    fun `should handle invalid JSON in create request`() = runTest {
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"invalid\": \"json\"}")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_REQUEST_BODY")
    }

    @Test
    @Order(16)
    fun `should handle case insensitive device states`() = runTest {
        // Test various case combinations
        listOf("inactive", "INACTIVE", "Inactive", "InAcTiVe").forEach { stateVariation ->
            webTestClient.get()
                .uri("/api/v1/devices/state/{state}", stateVariation)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
        }
    }

    @Test
    @Order(17)
    fun `should create and fetch devices with all valid states`() = runTest {
        // Create device with AVAILABLE state
        val availableDeviceRequest = CreateDeviceRequest(
            name = "Available Device Test",
            type = "Monitor",
            brand = "LG",
            state = DeviceState.AVAILABLE
        )

        val availableResult = webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(availableDeviceRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()

        val availableDeviceId = extractDeviceIdFromResponse(availableResult.responseBody!!)

        // Verify AVAILABLE state
        webTestClient.get()
            .uri("/api/v1/devices/state/available")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data[?(@.id == '$availableDeviceId')]").exists()
    }

    @Test
    @Order(18)
    fun `should partially update device fields`() = runTest {
        // Create a new device for partial update test
        val testDeviceRequest = CreateDeviceRequest(
            name = "Partial Update Test",
            type = "Keyboard",
            brand = "Logitech",
            state = DeviceState.INACTIVE
        )

        val createResult = webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(testDeviceRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()

        val testDeviceId = extractDeviceIdFromResponse(createResult.responseBody!!)

        // Update only the name
        val partialUpdateRequest = UpdateDeviceRequest(
            name = "Updated Keyboard Name",
            type = null,
            brand = null,
            state = null
        )

        webTestClient.put()
            .uri("/api/v1/devices/{id}", testDeviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(partialUpdateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.name").isEqualTo("Updated Keyboard Name")
            .jsonPath("$.data.type").isEqualTo("Keyboard") // Should remain unchanged
            .jsonPath("$.data.brand").isEqualTo("Logitech") // Should remain unchanged
            .jsonPath("$.data.state").isEqualTo("INACTIVE") // Should remain unchanged
    }

    @Test
    @Order(19)
    fun `should delete device successfully when not in use`() = runTest {
        val deviceId = createTestDevice(state = DeviceState.AVAILABLE)

        // Delete the device
        webTestClient.delete()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isNoContent

        // Verify device is deleted
        webTestClient.get()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @Order(20)
    fun `should verify database cleanup and final state`() = runTest {
        // Since database is cleared before each test, this should be empty
        webTestClient.get()
            .uri("/api/v1/devices")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(0)
    }

    // Helper function to extract device ID from JSON response
    private fun extractDeviceIdFromResponse(responseBody: String): String? {
        val regex = "\"id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(responseBody)?.groupValues?.get(1)
    }
}
