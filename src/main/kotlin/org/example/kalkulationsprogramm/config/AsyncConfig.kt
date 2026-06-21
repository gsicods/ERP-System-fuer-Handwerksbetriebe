package org.example.kalkulationsprogramm.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.Executor

@Configuration
class AsyncConfig {

    @Bean(name = ["emailTaskExecutor"])
    fun emailTaskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 200
        executor.setThreadNamePrefix("email-sync-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["defaultTaskExecutor"])
    @Primary
    fun taskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 16
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("async-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }

    @Bean(name = ["applicationTaskExecutor"])
    fun applicationTaskExecutor(
        @Qualifier("defaultTaskExecutor") executor: ThreadPoolTaskExecutor
    ): Executor = executor.threadPoolExecutor

    @Bean(name = ["taskScheduler"])
    fun taskScheduler(): TaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 4
        scheduler.setThreadNamePrefix("scheduled-")
        scheduler.setWaitForTasksToCompleteOnShutdown(true)
        scheduler.setAwaitTerminationSeconds(30)
        scheduler.initialize()
        return scheduler
    }
}
