package io.github.tonytonycoder11.agentictaskmanager.di

import javax.inject.Qualifier

/** Marks the long-lived, application-wide [kotlinx.coroutines.CoroutineScope] (used for seeding). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The IO [kotlinx.coroutines.CoroutineDispatcher], injected so tests can substitute a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
