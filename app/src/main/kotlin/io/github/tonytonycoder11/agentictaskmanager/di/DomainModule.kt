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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides the domain building blocks. The use cases live in the pure-Kotlin `:domain` module and
 * therefore have NO Hilt annotations of their own; we construct them here with @Provides. This is
 * what keeps the domain free of any DI-framework dependency.
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

    /**
     * A single, app-wide lock shared by every write use case. Because all mutations take this same
     * lock, their read-check-write sequences are serialized, which is what actually preserves the
     * acyclicity invariant under concurrent callers (UI today, agent calls in Phase 2).
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
