package io.github.tonytonycoder11.agentictaskmanager.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.tonytonycoder11.agentictaskmanager.data.AtmDatabase
import io.github.tonytonycoder11.agentictaskmanager.data.dao.DependencyDao
import io.github.tonytonycoder11.agentictaskmanager.data.dao.TaskDao
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AtmDatabase =
        Room.databaseBuilder(context, AtmDatabase::class.java, AtmDatabase.NAME).build()

    @Provides
    fun provideTaskDao(database: AtmDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideDependencyDao(database: AtmDatabase): DependencyDao = database.dependencyDao()
}
