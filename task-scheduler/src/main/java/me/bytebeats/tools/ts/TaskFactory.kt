package me.bytebeats.tools.ts

/**
 * Created by bytebeats on 2022/2/7 : 19:22
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Task factory
 * A factory to create a Task with a task name
 */
interface TaskFactory {
    fun create(taskName: String): Task
}