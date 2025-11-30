package com.matrix.multigpt.di

import com.matrix.multigpt.data.network.ModelFetchService
import com.matrix.multigpt.data.network.ModelFetchServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ModelFetchModule {
    
    @Binds
    @Singleton
    abstract fun bindModelFetchService(
        modelFetchServiceImpl: ModelFetchServiceImpl
    ): ModelFetchService
}
