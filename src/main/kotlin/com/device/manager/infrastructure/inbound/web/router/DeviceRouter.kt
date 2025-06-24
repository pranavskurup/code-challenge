package com.device.manager.infrastructure.inbound.web.router

import com.device.manager.infrastructure.inbound.web.handler.DeviceHandler
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class DeviceRouter(
    private val deviceHandler: DeviceHandler
) {
    
    private val logger = LoggerFactory.getLogger(DeviceRouter::class.java)

    @Bean
    fun deviceRoutes() = coRouter {
        logger.info("Configuring device routes")
        
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
        }.also {
            logger.info("Device routes configured successfully")
        }
    }
}
