package io.github.tonytonycoder11.agentictaskmanager.di

import javax.inject.Qualifier

/** Marks the long-lived, application-wide [kotlinx.coroutines.CoroutineScope] (used for seeding). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
