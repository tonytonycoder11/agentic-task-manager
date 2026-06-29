package io.github.tonytonycoder11.agentictaskmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.tonytonycoder11.agentictaskmanager.data.TaskRepositoryImpl
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import javax.inject.Singleton

/** Binds the domain repository interface to its Room-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
