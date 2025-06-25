package com.device.manager.infrastructure.outbound.persistence.config

import io.r2dbc.spi.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.device.manager.infrastructure.outbound.persistence.repository"])
@EnableTransactionManagement
class R2dbcConfiguration(
    private val connectionFactory: ConnectionFactory
) : AbstractR2dbcConfiguration() {

    private val logger = LoggerFactory.getLogger(R2dbcConfiguration::class.java)

    override fun connectionFactory(): ConnectionFactory {
        logger.info("Configuring R2DBC connection factory")
        return connectionFactory
    }

    @Bean
    fun transactionManager(): ReactiveTransactionManager {
        logger.info("Configuring R2DBC transaction manager")
        return R2dbcTransactionManager(connectionFactory)
    }

    @Bean
    override fun r2dbcCustomConversions(): R2dbcCustomConversions {
        logger.info("Configuring R2DBC custom conversions")
        return R2dbcCustomConversions(
            R2dbcCustomConversions.STORE_CONVERSIONS,
            emptyList<org.springframework.core.convert.converter.Converter<*, *>>()
        ).also {
            logger.debug("R2DBC custom conversions configured successfully")
        }
    }
}
