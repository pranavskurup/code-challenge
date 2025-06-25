package com.device.manager

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<DeviceManagerApplication>().with(TestcontainersConfiguration::class).run(*args)
}
