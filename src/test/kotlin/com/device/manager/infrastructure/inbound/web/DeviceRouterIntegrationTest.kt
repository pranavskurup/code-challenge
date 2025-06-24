package com.device.manager.infrastructure.inbound.web

import com.device.manager.application.service.DeviceService
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.DeviceResponse
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import arrow.core.left
import arrow.core.right
import com.device.manager.infrastructure.inbound.web.handler.DeviceHandler
import com.device.manager.infrastructure.inbound.web.router.DeviceRouter
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.ZonedDateTime
import java.util.UUID

@WebFluxTest
@Import(DeviceRouterIntegrationTest.TestConfig::class)
class DeviceRouterIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var deviceService: DeviceService

    @TestConfiguration
    class TestConfig {
        @Bean
        fun deviceService(): DeviceService = mockk()

        @Bean
        fun deviceHandler(deviceService: DeviceService) =
            DeviceHandler(deviceService)

        @Bean
        fun deviceRouter(deviceHandler: com.device.manager.infrastructure.inbound.web.handler.DeviceHandler) =
            DeviceRouter(deviceHandler).deviceRoutes()
    }

    private val deviceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val deviceResponse = DeviceResponse(
        id = deviceId,
        name = "iPhone 15 Pro",
        type = "Smartphone",
        brand = "Apple",
        state = DeviceState.INACTIVE,
        creationTime = ZonedDateTime.now().toString(),
        updatedTime = ZonedDateTime.now().toString()
    )

    @Test
    fun `should create device successfully`() = runTest {
        // Given
        val createRequest = CreateDeviceRequest(
            name = "iPhone 15 Pro",
            type = "Smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE
        )

        coEvery { deviceService.execute(any<CreateDeviceRequest>()) } returns deviceResponse.right()

        // When & Then
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
            .jsonPath("$.data.id").isEqualTo(deviceId.toString())
            .jsonPath("$.data.name").isEqualTo("iPhone 15 Pro")
    }

    @Test
    fun `should return bad request when create device with invalid body`() = runTest {
        // When & Then
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"invalid\": \"body\"}")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `should get device by id successfully`() = runTest {
        // Given
        coEvery { deviceService.execute(deviceId) } returns deviceResponse.right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.id").isEqualTo(deviceId.toString())
            .jsonPath("$.data.name").isEqualTo("iPhone 15 Pro")
    }

    @Test
    fun `should return not found when device does not exist`() = runTest {
        // Given
        coEvery { deviceService.execute(deviceId) } returns
            DomainError.DeviceError.DeviceNotFound("Device with id $deviceId not found").left()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("DEVICE_NOT_FOUND")
    }

    @Test
    fun `should return bad request for invalid uuid`() = runTest {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/invalid-uuid")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_UUID")
    }

    @Test
    fun `should update device successfully`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "iPhone 15 Pro Max",
            type = null,
            brand = null,
            state = DeviceState.AVAILABLE
        )

        val updatedDevice = deviceResponse.copy(
            name = "iPhone 15 Pro Max",
            state = DeviceState.AVAILABLE
        )

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns updatedDevice.right()

        // When & Then
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
            .jsonPath("$.data.name").isEqualTo("iPhone 15 Pro Max")
            .jsonPath("$.data.state").isEqualTo("AVAILABLE")
    }

    @Test
    fun `should delete device successfully`() = runTest {
        // Given
        coEvery { deviceService.deleteDevice(deviceId) } returns Unit.right()

        // When & Then
        webTestClient.delete()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    fun `should get all devices successfully`() = runTest {
        // Given
        coEvery { deviceService.execute() } returns listOf(deviceResponse).right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data[0].id").isEqualTo(deviceId.toString())
    }

    @Test
    fun `should get devices by brand successfully`() = runTest {
        // Given
        val brand = "Apple"
        coEvery { deviceService.execute(brand) } returns listOf(deviceResponse).right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/brand/{brand}", brand)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data[0].brand").isEqualTo("Apple")
    }

    @Test
    fun `should return bad request for empty brand`() = runTest {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/brand/ ")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_BRAND")
    }

    @Test
    fun `should get devices by state successfully`() = runTest {
        // Given
        coEvery { deviceService.execute(DeviceState.INACTIVE) } returns listOf(deviceResponse).right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/state/inactive")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data[0].state").isEqualTo("INACTIVE")
    }

    @Test
    fun `should return bad request for invalid state`() = runTest {
        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/state/invalid-state")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("INVALID_STATE")
    }

    // Additional Domain Validation Test Cases

    @Test
    fun `should return conflict when creating device with existing name and brand`() = runTest {
        // Given
        val createRequest = CreateDeviceRequest(
            name = "iPhone 15 Pro",
            type = "Smartphone",
            brand = "Apple",
            state = DeviceState.INACTIVE
        )

        coEvery { deviceService.execute(any<CreateDeviceRequest>()) } returns
            DomainError.DeviceError.DeviceAlreadyExists("Device with name 'iPhone 15 Pro' and brand 'Apple' already exists").left()

        // When & Then
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("DEVICE_ALREADY_EXISTS")
            .jsonPath("$.message").isEqualTo("Device with name 'iPhone 15 Pro' and brand 'Apple' already exists")
    }

    @Test
    fun `should prevent updating creation time`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "Updated Device",
            type = "Updated Type",
            brand = "Updated Brand",
            state = DeviceState.AVAILABLE
        )

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns
            DomainError.DeviceError.CreationTimeImmutable("Creation time cannot be updated").left()

        // When & Then
        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("CREATION_TIME_IMMUTABLE")
            .jsonPath("$.message").isEqualTo("Creation time cannot be updated")
    }

    @Test
    fun `should prevent updating name and brand when device is in use`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "New Name",
            type = null,
            brand = "New Brand",
            state = null
        )

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns
            DomainError.DeviceError.InUseDeviceImmutableFields("Name and brand cannot be updated when device is in use").left()

        // When & Then
        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("IN_USE_DEVICE_IMMUTABLE_FIELDS")
            .jsonPath("$.message").isEqualTo("Name and brand cannot be updated when device is in use")
    }

    @Test
    fun `should prevent deleting device that is in use`() = runTest {
        // Given
        coEvery { deviceService.deleteDevice(deviceId) } returns
            DomainError.DeviceError.InUseDeviceCannotBeDeleted("Device cannot be deleted while it is in use").left()

        // When & Then
        webTestClient.delete()
            .uri("/api/v1/devices/{id}", deviceId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("IN_USE_DEVICE_CANNOT_BE_DELETED")
            .jsonPath("$.message").isEqualTo("Device cannot be deleted while it is in use")
    }

    @Test
    fun `should handle device state change errors`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = null,
            type = null,
            brand = null,
            state = DeviceState.IN_USE
        )

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns
            DomainError.DeviceError.DeviceStateChangeError(deviceId.toString(), DeviceState.INACTIVE).left()

        // When & Then
        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("DEVICE_STATE_CHANGE_ERROR")
    }

    // Edge Cases and Error Scenarios

    @Test
    fun `should handle empty device list when fetching all devices`() = runTest {
        // Given
        coEvery { deviceService.execute() } returns emptyList<DeviceResponse>().right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data").isEmpty
    }

    @Test
    fun `should handle empty device list when fetching by brand`() = runTest {
        // Given
        val brand = "NonExistentBrand"
        coEvery { deviceService.execute(brand) } returns emptyList<DeviceResponse>().right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/brand/{brand}", brand)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data").isEmpty
    }

    @Test
    fun `should handle empty device list when fetching by state`() = runTest {
        // Given
        coEvery { deviceService.execute(DeviceState.IN_USE) } returns emptyList<DeviceResponse>().right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/state/in-use")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data").isEmpty
    }

    @Test
    fun `should create device with all states - inactive`() = runTest {
        // Given
        val createRequest = CreateDeviceRequest(
            name = "Inactive Device",
            type = "Laptop",
            brand = "Dell",
            state = DeviceState.INACTIVE
        )

        val response = deviceResponse.copy(
            name = "Inactive Device",
            type = "Laptop",
            brand = "Dell",
            state = DeviceState.INACTIVE
        )

        coEvery { deviceService.execute(any<CreateDeviceRequest>()) } returns response.right()

        // When & Then
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.data.state").isEqualTo("INACTIVE")
    }

    @Test
    fun `should create device with available state`() = runTest {
        // Given
        val createRequest = CreateDeviceRequest(
            name = "Available Device",
            type = "Tablet",
            brand = "Samsung",
            state = DeviceState.AVAILABLE
        )

        val response = deviceResponse.copy(
            name = "Available Device",
            type = "Tablet",
            brand = "Samsung",
            state = DeviceState.AVAILABLE
        )

        coEvery { deviceService.execute(any<CreateDeviceRequest>()) } returns response.right()

        // When & Then
        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.data.state").isEqualTo("AVAILABLE")
    }

    @Test
    fun `should partially update device - only name`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = "Updated Name Only",
            type = null,
            brand = null,
            state = null
        )

        val updatedDevice = deviceResponse.copy(name = "Updated Name Only")

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns updatedDevice.right()

        // When & Then
        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.name").isEqualTo("Updated Name Only")
            .jsonPath("$.data.type").isEqualTo("Smartphone") // Should remain unchanged
            .jsonPath("$.data.brand").isEqualTo("Apple") // Should remain unchanged
    }

    @Test
    fun `should partially update device - only state`() = runTest {
        // Given
        val updateRequest = UpdateDeviceRequest(
            name = null,
            type = null,
            brand = null,
            state = DeviceState.AVAILABLE
        )

        val updatedDevice = deviceResponse.copy(state = DeviceState.AVAILABLE)

        coEvery { deviceService.execute(deviceId, any<UpdateDeviceRequest>()) } returns updatedDevice.right()

        // When & Then
        webTestClient.put()
            .uri("/api/v1/devices/{id}", deviceId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.state").isEqualTo("AVAILABLE")
            .jsonPath("$.data.name").isEqualTo("iPhone 15 Pro") // Should remain unchanged
    }

    @Test
    fun `should fetch devices by all available states`() = runTest {
        // Test INACTIVE state
        coEvery { deviceService.execute(DeviceState.INACTIVE) } returns listOf(
            deviceResponse.copy(state = DeviceState.INACTIVE)
        ).right()

        webTestClient.get()
            .uri("/api/v1/devices/state/inactive")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data[0].state").isEqualTo("INACTIVE")

        // Test AVAILABLE state
        coEvery { deviceService.execute(DeviceState.AVAILABLE) } returns listOf(
            deviceResponse.copy(state = DeviceState.AVAILABLE)
        ).right()

        webTestClient.get()
            .uri("/api/v1/devices/state/available")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data[0].state").isEqualTo("AVAILABLE")

        // Test IN_USE state
        coEvery { deviceService.execute(DeviceState.IN_USE) } returns listOf(
            deviceResponse.copy(state = DeviceState.IN_USE)
        ).right()

        webTestClient.get()
            .uri("/api/v1/devices/state/in-use")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data[0].state").isEqualTo("IN_USE")
    }

    @Test
    fun `should handle multiple devices with same brand`() = runTest {
        // Given
        val brand = "Apple"
        val devices = listOf(
            deviceResponse.copy(id = UUID.randomUUID(), name = "iPhone 15"),
            deviceResponse.copy(id = UUID.randomUUID(), name = "iPhone 15 Pro"),
            deviceResponse.copy(id = UUID.randomUUID(), name = "iPhone 15 Pro Max")
        )

        coEvery { deviceService.execute(brand) } returns devices.right()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices/brand/{brand}", brand)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(3)
            .jsonPath("$.data[0].brand").isEqualTo("Apple")
            .jsonPath("$.data[1].brand").isEqualTo("Apple")
            .jsonPath("$.data[2].brand").isEqualTo("Apple")
    }

    @Test
    fun `should handle case insensitive state parameter`() = runTest {
        // Given
        coEvery { deviceService.execute(DeviceState.INACTIVE) } returns listOf(deviceResponse).right()

        // Test different case variations
        listOf("INACTIVE", "inactive", "Inactive", "InAcTiVe").forEach { stateVariation ->
            webTestClient.get()
                .uri("/api/v1/devices/state/{state}", stateVariation)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data[0].state").isEqualTo("INACTIVE")
        }
    }

    @Test
    fun `should return proper error for malformed UUID`() = runTest {
        val malformedUuids = listOf("123", "not-a-uuid", "123e4567-e89b")

        malformedUuids.forEach { malformedUuid ->
            webTestClient.get()
                .uri("/api/v1/devices/{id}", malformedUuid)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_UUID")
        }
    }

    @Test
    fun `should return not found for empty UUID path`() = runTest {
        // When accessing with empty UUID, it should not match the route pattern
        webTestClient.get()
            .uri("/api/v1/devices/")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should handle service errors gracefully`() = runTest {
        // Given
        coEvery { deviceService.execute() } returns
            DomainError.DeviceError("SERVICE_ERROR", "Database connection failed").left()

        // When & Then
        webTestClient.get()
            .uri("/api/v1/devices")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.code").isEqualTo("SERVICE_ERROR")
            .jsonPath("$.message").isEqualTo("Database connection failed")
    }

    @Test
    fun `should validate required fields in create request`() = runTest {
        // Test with missing name
        val invalidRequest1 = mapOf(
            "type" to "Smartphone",
            "brand" to "Apple"
        )

        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest1)
            .exchange()
            .expectStatus().isBadRequest

        // Test with empty name
        val invalidRequest2 = CreateDeviceRequest(
            name = "",
            type = "Smartphone",
            brand = "Apple"
        )

        coEvery { deviceService.execute(any<CreateDeviceRequest>()) } returns
            DomainError.DeviceError("VALIDATION_ERROR", "Name cannot be empty").left()

        webTestClient.post()
            .uri("/api/v1/devices")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest2)
            .exchange()
            .expectStatus().isBadRequest
    }
}
