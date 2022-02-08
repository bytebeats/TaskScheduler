package me.bytebeats.tools.ts

/**
 * Created by bytebeats on 2022/2/7 : 19:25
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class TaskStore(private val factory: TaskFactory) {
    private val mTasks = mutableMapOf<String, Task>()

    @Throws(IllegalArgumentException::class)
    @Synchronized
    operator fun get(taskName: String): Task {
        var task = mTasks[taskName]
        if (task == null) {
            task = factory.create(taskName)

            if (task == null) {
                throw IllegalArgumentException("Create task fail, there is no task corresponding to the task name. Make sure you have create a task instance in TaskFactory.")
            }

            mTasks[taskName] = task
        }
        return task
    }
}