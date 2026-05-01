package com.housemonitor.di

import com.housemonitor.data.remote.ShzlApiService
import com.housemonitor.data.repository.OtaRepository
import com.housemonitor.data.repository.OtaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OtaModule {

    @Binds
    @Singleton
    abstract fun bindOtaRepository(impl: OtaRepositoryImpl): OtaRepository

    companion object {
        @Provides
        @Singleton
        fun provideShzlApiService(): ShzlApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://shz.al/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ShzlApiService::class.java)
        }
    }
}
