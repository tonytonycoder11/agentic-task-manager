package io.github.tonytonycoder11.agentictaskmanager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.support.IdGenerator
import io.github.tonytonycoder11.agentictaskmanager.domain.support.UuidIdGenerator
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddDependencyUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.CompleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.DeleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetActionableTasksUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetBlockingOverdueUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetTaskInsightUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.ObserveTaskBoardUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides the domain building blocks. Use cases carry no Hilt annotations, keeping the pure-Kotlin
 * `:domain` module free of any DI-framework dependency; they are constructed here with @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * A single app-wide lock shared by every write use case, serializing their read-check-write
     * sequences so the acyclicity invariant holds under concurrent callers.
     */
    @Provides
    @Singleton
    fun provideMutationLock(): Mutex = Mutex()

    @Provides
    fun provideAddTaskUseCase(
        repository: TaskRepository,
        idGenerator: IdGenerator,
        mutationLock: Mutex,
    ): AddTaskUseCase = AddTaskUseCase(repository, idGenerator, mutationLock)

    @Provides
    fun provideObserveTaskBoardUseCase(repository: TaskRepository): ObserveTaskBoardUseCase =
        ObserveTaskBoardUseCase(repository)

    @Provides
    fun provideAddDependencyUseCase(
        repository: TaskRepository,
        mutationLock: Mutex,
    ): AddDependencyUseCase = AddDependencyUseCase(repository, mutationLock)

    @Provides
    fun provideCompleteTaskUseCase(
        repository: TaskRepository,
        idGenerator: IdGenerator,
        clock: Clock,
        mutationLock: Mutex,
    ): CompleteTaskUseCase = CompleteTaskUseCase(repository, idGenerator, clock, mutationLock)

    @Provides
    fun provideDeleteTaskUseCase(
        repository: TaskRepository,
        mutationLock: Mutex,
    ): DeleteTaskUseCase = DeleteTaskUseCase(repository, mutationLock)

    @Provides
    fun provideGetActionableTasksUseCase(repository: TaskRepository): GetActionableTasksUseCase =
        GetActionableTasksUseCase(repository)

    @Provides
    fun provideGetBlockingOverdueUseCase(
        repository: TaskRepository,
        clock: Clock,
    ): GetBlockingOverdueUseCase = GetBlockingOverdueUseCase(repository, clock)

    @Provides
    fun provideGetTaskInsightUseCase(repository: TaskRepository): GetTaskInsightUseCase =
        GetTaskInsightUseCase(repository)
}
