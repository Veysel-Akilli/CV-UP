// com/example/myapplication/di/LocalModule.kt
package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.local.CvDraftStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {
    @Provides @Singleton
    fun provideCvDraftStore(@ApplicationContext ctx: Context): CvDraftStore = CvDraftStore(ctx)
}
