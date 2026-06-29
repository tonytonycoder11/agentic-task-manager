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
 * It also implements [AppFunctionConfiguration.Provider]: because the @AppFunction classes have
 * constructor dependencies (the domain use cases), the system can't instantiate them itself, so we
 * provide a factory that hands it the Hilt-built instances. This is the Hilt <-> AppFunctions bridge.
 */
@HiltAndroidApp
class AtmApplication : Application(), AppFunctionConfiguration.Provider {

    @Inject
    lateinit var seeder: DatabaseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    // Built by Hilt with all their use-case dependencies; the factory below hands them to the system.
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
