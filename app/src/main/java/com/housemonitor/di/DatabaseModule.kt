package com.housemonitor.di

import android.content.Context
import com.google.gson.Gson
import com.housemonitor.data.database.HouseMonitorDatabase
import com.housemonitor.data.database.MonitorRecordDao
import com.housemonitor.data.database.PropertyDao
import com.housemonitor.data.database.UserSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHouseMonitorDatabase(@ApplicationContext context: Context): HouseMonitorDatabase {
        return HouseMonitorDatabase.getDatabase(context)
    }

    @Provides
    fun providePropertyDao(database: HouseMonitorDatabase): PropertyDao {
        return database.propertyDao()
    }

    @Provides
    fun provideMonitorRecordDao(database: HouseMonitorDatabase): MonitorRecordDao {
        return database.monitorRecordDao()
    }

    @Provides
    fun provideUserSettingsDao(database: HouseMonitorDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}