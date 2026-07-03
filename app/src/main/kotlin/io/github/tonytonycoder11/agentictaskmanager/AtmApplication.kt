package io.github.tonytonycoder11.agentictaskmanager

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dagger.hilt.android.HiltAndroidApp
import io.github.tonytonycoder11.agentictaskmanager.agent.TaskActionFunctions
import io.github.tonytonycoder11.agentictaskmanager.agent.TaskQueryFunctions
import io.github.tonytonycoder11.agentictaskmanager.data.DatabaseSeeder
import io.github.tonytonycoder11.agentictaskmanager.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point and Hilt root.
 *
 * Bridges Hilt and AppFunctions: since the @AppFunction classes take constructor dependencies, the
 * system can't instantiate them, so [appFunctionConfiguration] hands it the Hilt-built instances.
 */
@HiltAndroidApp
class AtmApplication : Application(), AppFunctionConfiguration.Provider {

    @Inject
    lateinit var seeder: DatabaseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var taskQueryFunctions: TaskQueryFunctions

    @Inject
    lateinit var taskActionFunctions: TaskActionFunctions

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(TaskQueryFunctions::class.java) { taskQueryFunctions }
            .addEnclosingClassFactory(TaskActionFunctions::class.java) { taskActionFunctions }
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            seeder.seedIfEmpty()
        }
    }
}
