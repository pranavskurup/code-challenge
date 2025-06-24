package com.device.manager

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DeviceManagerApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(DeviceManagerApplication::class.java)
    
    try {
        logger.info("Starting Device Manager Application...")
        logger.debug("Application arguments: {}", args.joinToString())
        
        runApplication<DeviceManagerApplication>(*args)
        
        logger.info("Device Manager Application started successfully")
    } catch (e: Exception) {
        logger.error("Failed to start Device Manager Application", e)
        throw e
    }
}
