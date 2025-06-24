package com.device.manager.infrastructure.inbound.web.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.device.manager.application.service.DeviceService
import com.device.manager.application.service.dto.CreateDeviceRequest
import com.device.manager.application.service.dto.UpdateDeviceRequest
import com.device.manager.domain.entity.DeviceState
import com.device.manager.domain.errors.DomainError
import com.device.manager.infrastructure.inbound.web.dto.ApiResponse
import com.device.manager.infrastructure.inbound.web.dto.ErrorResponse
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class DeviceHandler(
    private val deviceService: DeviceService
) {
    
    private val logger = LoggerFactory.getLogger(DeviceHandler::class.java)

    /**
     * Create a new device
     * POST /api/v1/devices
     */
    suspend fun createDevice(request: ServerRequest): ServerResponse {
        logger.info("Received request to create new device")
        
        return try {
            val createRequest = request.awaitBodyOrNull<CreateDeviceRequest>()
                ?: run {
                    logger.warn("Invalid or missing request body for device creation")
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid or missing request body"))
                }

            logger.debug("Creating device with request: {}", createRequest)

            deviceService.execute(createRequest).fold(
                { error -> 
                    logger.warn("Failed to create device: {} - {}", error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device -> 
                    logger.info("Device created successfully with ID: {}", device.id)
                    ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device, "Device created successfully"))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while creating device", e)
            when {
                e.message?.contains("Failed to read HTTP message") == true ||
                e.message?.contains("JSON") == true ||
                e.message?.contains("parse") == true -> {
                    logger.warn("Invalid JSON format in request body")
                    ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid JSON format"))
                }
                else -> buildInternalErrorResponse(e)
            }
        }
    }

    /**
     * Get device by ID
     * GET /api/v1/devices/{id}
     */
    suspend fun getDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to get device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)
            logger.debug("Parsed device ID: {}", id)

            deviceService.execute(id).fold(
                { error -> 
                    logger.warn("Failed to get device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device ->
                    logger.debug("Successfully retrieved device: {}", device.id)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device))
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while getting device with ID: {}", deviceId, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Update device
     * PUT /api/v1/devices/{id}
     */
    suspend fun updateDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to update device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)
            val updateRequest = request.awaitBodyOrNull<UpdateDeviceRequest>()
                ?: run {
                    logger.warn("Invalid or missing request body for device update")
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid or missing request body"))
                }

            logger.debug("Updating device {} with request: {}", id, updateRequest)

            deviceService.execute(id, updateRequest).fold(
                { error -> 
                    logger.warn("Failed to update device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { device ->
                    logger.info("Device updated successfully: {}", device.id)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(device, "Device updated successfully"))
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided for update: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while updating device with ID: {}", deviceId, e)
            when {
                e.message?.contains("Failed to read HTTP message") == true ||
                e.message?.contains("JSON") == true ||
                e.message?.contains("parse") == true -> {
                    logger.warn("Invalid JSON format in update request body")
                    ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_REQUEST_BODY", "Invalid JSON format"))
                }
                else -> buildInternalErrorResponse(e)
            }
        }
    }

    /**
     * Delete device
     * DELETE /api/v1/devices/{id}
     */
    suspend fun deleteDevice(request: ServerRequest): ServerResponse {
        val deviceId = request.pathVariable("id")
        logger.info("Received request to delete device with ID: {}", deviceId)
        
        return try {
            val id = UUID.fromString(deviceId)

            deviceService.deleteDevice(id).fold(
                { error -> 
                    logger.warn("Failed to delete device with ID {}: {} - {}", id, error.code, error.message)
                    buildErrorResponse(error) 
                },
                {
                    logger.info("Device deleted successfully: {}", id)
                    ServerResponse.noContent().buildAndAwait()
                }
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid UUID format provided for deletion: {}", deviceId)
            ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(ErrorResponse("INVALID_UUID", "Invalid device ID format"))
        } catch (e: Exception) {
            logger.error("Unexpected error while deleting device with ID: {}", deviceId, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get all devices
     * GET /api/v1/devices
     */
    suspend fun getAllDevices(request: ServerRequest): ServerResponse {
        logger.info("Received request to get all devices")
        
        return try {
            deviceService.execute().fold(
                { error -> 
                    logger.warn("Failed to get all devices: {} - {}", error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices", devices.size)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting all devices", e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get devices by brand
     * GET /api/v1/devices/brand/{brand}
     */
    suspend fun getDevicesByBrand(request: ServerRequest): ServerResponse {
        val brand = request.pathVariable("brand")
        logger.info("Received request to get devices by brand: {}", brand)
        
        return try {
            if (brand.isBlank()) {
                logger.warn("Empty brand parameter provided")
                return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValueAndAwait(ErrorResponse("INVALID_BRAND", "Brand cannot be empty"))
            }

            deviceService.execute(brand).fold(
                { error -> 
                    logger.warn("Failed to get devices by brand '{}': {} - {}", brand, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices for brand: {}", devices.size, brand)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting devices by brand: {}", brand, e)
            buildInternalErrorResponse(e)
        }
    }

    /**
     * Get devices by state
     * GET /api/v1/devices/state/{state}
     */
    suspend fun getDevicesByState(request: ServerRequest): ServerResponse {
        val stateParam = request.pathVariable("state")
        logger.info("Received request to get devices by state: {}", stateParam)
        
        return try {
            val state = DeviceState.entries.find { it.state.equals(stateParam, ignoreCase = true) }
                ?: run {
                    logger.warn("Invalid device state provided: {}", stateParam)
                    return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ErrorResponse("INVALID_STATE", "Invalid device state. Valid states: ${DeviceState.entries.map { it.state }}"))
                }

            logger.debug("Parsed device state: {}", state)

            deviceService.execute(state).fold(
                { error -> 
                    logger.warn("Failed to get devices by state '{}': {} - {}", state, error.code, error.message)
                    buildErrorResponse(error) 
                },
                { devices ->
                    logger.info("Successfully retrieved {} devices for state: {}", devices.size, state)
                    ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValueAndAwait(ApiResponse.success(devices))
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error while getting devices by state: {}", stateParam, e)
            buildInternalErrorResponse(e)
        }
    }

    private suspend fun buildErrorResponse(error: DomainError.DeviceError): ServerResponse {
        logger.debug("Building error response for: {} - {}", error.code, error.message)
        
        val status = when (error.code) {
            DomainError.DeviceError.DEVICE_NOT_FOUND -> HttpStatus.NOT_FOUND
            DomainError.DeviceError.DEVICE_ALREADY_EXISTS -> HttpStatus.CONFLICT
            DomainError.DeviceError.DEVICE_UNAUTHORIZED -> HttpStatus.FORBIDDEN
            DomainError.DeviceError.DEVICE_STATE_CHANGE_ERROR,
            DomainError.DeviceError.CREATION_TIME_IMMUTABLE,
            DomainError.DeviceError.IN_USE_DEVICE_IMMUTABLE_FIELDS,
            DomainError.DeviceError.IN_USE_DEVICE_CANNOT_BE_DELETED,
            "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ErrorResponse(error.code ?: "UNKNOWN_ERROR", error.message ?: "An error occurred"))
    }

    private suspend fun buildInternalErrorResponse(e: Exception): ServerResponse {
        logger.error("Building internal server error response", e)
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred: ${e.message}"))
    }
}
