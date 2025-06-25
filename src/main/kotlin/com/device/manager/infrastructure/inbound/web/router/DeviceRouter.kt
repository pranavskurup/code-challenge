package com.device.manager.infrastructure.inbound.web.router

import com.device.manager.infrastructure.inbound.web.handler.DeviceHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
@Tag(name = "Device Management", description = "Device Management and Query Operations")
class DeviceRouter(
    private val deviceHandler: DeviceHandler
) {

    private val logger = LoggerFactory.getLogger(DeviceRouter::class.java)

    init {
        logger.info("DeviceRouter initialized - router functions are now configured in RouterFunctionConfig")
    }


    @Bean
    @RouterOperations(
        RouterOperation(
            path = "/api/v1/devices",
            method = [RequestMethod.POST],
            beanClass = DeviceHandler::class,
            beanMethod = "createDevice"
        ),
        RouterOperation(
            path = "/api/v1/devices",
            method = [RequestMethod.GET],
            beanClass = DeviceHandler::class,
            beanMethod = "getAllDevices"
        ),
        RouterOperation(
            path = "/api/v1/devices/{id}",
            method = [RequestMethod.GET],
            beanClass = DeviceHandler::class,
            beanMethod = "getDevice"
        ),
        RouterOperation(
            path = "/api/v1/devices/{id}",
            method = [RequestMethod.PUT],
            beanClass = DeviceHandler::class,
            beanMethod = "updateDevice"
        ),
        RouterOperation(
            path = "/api/v1/devices/{id}",
            method = [RequestMethod.DELETE],
            beanClass = DeviceHandler::class,
            beanMethod = "deleteDevice"
        ),
        RouterOperation(
            path = "/api/v1/devices/brand/{brand}",
            method = [RequestMethod.GET],
            beanClass = DeviceHandler::class,
            beanMethod = "getDevicesByBrand"
        ),
        RouterOperation(
            path = "/api/v1/devices/state/{state}",
            method = [RequestMethod.GET],
            beanClass = DeviceHandler::class,
            beanMethod = "getDevicesByState"
        )
    )
    fun deviceRoutes(): RouterFunction<ServerResponse> = coRouter {
        "/api/v1/devices".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                // Device CRUD operations
                POST("", deviceHandler::createDevice)
                GET("", deviceHandler::getAllDevices)
                GET("/{id}", deviceHandler::getDevice)
                PUT("/{id}", deviceHandler::updateDevice)
                DELETE("/{id}", deviceHandler::deleteDevice)

                // Query operations
                GET("/brand/{brand}", deviceHandler::getDevicesByBrand)
                GET("/state/{state}", deviceHandler::getDevicesByState)
            }
        }
    }
}
