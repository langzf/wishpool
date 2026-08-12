package com.wishpool.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class CoreBeans {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
