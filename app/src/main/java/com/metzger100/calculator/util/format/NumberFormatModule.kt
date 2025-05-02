package com.metzger100.calculator.util.format

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NumberFormatModule {
    @Provides
    @Singleton
    fun provideNumberFormatService(): NumberFormatService = NumberFormatService()
}