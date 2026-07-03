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
 * Seeds a small synthetic scenario on first run; the data is intentionally shaped to exercise
 * every domain feature (dependencies, overdue, recurrence). Uses only synthetic data — no real
 * personal data. Goes through the real [AddTaskUseCase] so dependency links are validated exactly
 * as at runtime.
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
                dueAt = now.minus(1, ChronoUnit.DAYS),
            ),
        )
        // Depends on both of the above, so it starts blocked.
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
