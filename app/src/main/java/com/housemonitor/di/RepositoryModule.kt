package com.housemonitor.di

import com.housemonitor.data.repository.MonitorRepository
import com.housemonitor.data.repository.PropertyRepository
import com.housemonitor.data.repository.UserSettingsRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are already annotated with @Singleton
    // No additional provides needed as they use constructor injection
}