package com.housemonitor

import com.housemonitor.service.OtaManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OtaEntryPoint {
    fun otaManager(): OtaManager
}
