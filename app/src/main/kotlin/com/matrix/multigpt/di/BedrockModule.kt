package com.matrix.multigpt.di

import com.matrix.multigpt.data.network.BedrockAPI
import com.matrix.multigpt.data.network.BedrockAPIImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BedrockModule {

    @Binds
    @Singleton
    abstract fun bindBedrockAPI(
        bedrockAPIImpl: BedrockAPIImpl
    ): BedrockAPI
}
