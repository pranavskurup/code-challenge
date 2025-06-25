package com.device.manager.infrastructure.inbound.web.exception

import com.device.manager.infrastructure.inbound.web.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.server.ResponseStatusException

@Component
@Order(-2)
class GlobalExceptionHandler(
    private val objectMapper: ObjectMapper
) : ErrorWebExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val response = exchange.response
        val errorResponse = when (ex) {
            is ResponseStatusException -> {
                response.statusCode = ex.statusCode
                ErrorResponse(
                    code = ex.statusCode.toString(),
                    message = ex.reason ?: "An error occurred"
                )
            }
            is IllegalArgumentException -> {
                response.statusCode = HttpStatus.BAD_REQUEST
                ErrorResponse(
                    code = "BAD_REQUEST",
                    message = ex.message ?: "Invalid request parameters"
                )
            }
            is RuntimeException -> {
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                logger.error("Unexpected runtime exception", ex)
                ErrorResponse(
                    code = "INTERNAL_SERVER_ERROR",
                    message = "An unexpected error occurred"
                )
            }
            else -> {
                response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                logger.error("Unexpected exception", ex)
                ErrorResponse(
                    code = "INTERNAL_SERVER_ERROR",
                    message = "An unexpected error occurred"
                )
            }
        }

        response.headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)

        return try {
            val bytes = objectMapper.writeValueAsBytes(errorResponse)
            val buffer: DataBuffer = response.bufferFactory().wrap(bytes)
            response.writeWith(Mono.just(buffer))
        } catch (e: Exception) {
            logger.error("Error writing response", e)
            response.writeWith(Mono.empty())
        }
    }
}
