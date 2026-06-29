package io.github.tonytonycoder11.agentictaskmanager.data

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskCommand
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskUseCase
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds a small, fully SYNTHETIC scenario the first time the app runs (security rule #5: no real
 * personal data). The data is intentionally shaped to exercise every domain feature so the app —
 * and later the agent — has something interesting to reason about:
 *
 *  - "Prepare slides" and "Book venue" both block "Present at the GDG meetup" (so it is blocked);
 *  - "Book venue" is already overdue and blocking, so it surfaces in getBlockingOverdue();
 *  - "Buy milk" is free (actionable); "Water the plants" is a weekly recurring task.
 *
 * Seeding goes through the real [AddTaskUseCase] (not raw inserts), so the dependency links are
 * created and validated exactly as they would be at runtime.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val repository: TaskRepository,
    private val addTask: AddTaskUseCase,
    private val clock: Clock,
) {
    suspend fun seedIfEmpty() {
        if (repository.getAllTasks().isNotEmpty()) return

        val now = clock.instant()

        addTask(
            AddTaskCommand(
                title = "Prepare slides",
                description = "Draft the deck for the AppFunctions talk.",
                priority = Priority.HIGH,
                dueAt = now.plus(2, ChronoUnit.DAYS),
            ),
        )
        addTask(
            AddTaskCommand(
                title = "Book venue",
                description = "Reserve the room for the meetup.",
                priority = Priority.URGENT,
                dueAt = now.minus(1, ChronoUnit.DAYS), // already overdue
            ),
        )
        // Depends on both of the above by title -> starts blocked.
        addTask(
            AddTaskCommand(
                title = "Present at the GDG meetup",
                priority = Priority.HIGH,
                dueAt = now.plus(5, ChronoUnit.DAYS),
                dependsOnTitles = listOf("Prepare slides", "Book venue"),
            ),
        )
        addTask(
            AddTaskCommand(
                title = "Buy milk",
                priority = Priority.LOW,
            ),
        )
        addTask(
            AddTaskCommand(
                title = "Water the plants",
                priority = Priority.MEDIUM,
                dueAt = now.plus(1, ChronoUnit.DAYS),
                recurrence = Recurrence.WEEKLY,
            ),
        )
    }
}
