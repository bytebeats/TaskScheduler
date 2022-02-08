package me.bytebeats.tools.ts.parser

import me.bytebeats.tools.ts.Project
import me.bytebeats.tools.ts.Task
import me.bytebeats.tools.ts.TaskScheduler
import java.io.InputStream

/**
 * Created by bytebeats on 2022/2/8 : 17:45
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
interface ITaskParser {
    /**
     * Parse Project from xml or json file
     *
     * @param inputStream xml or json input stream
     * @return project list
     */
    fun parse(inputStream: InputStream): List<ProjectInfo>

    /**
     * Project info describes a Project which is declared in xml or json file
     * parse xml or json file into Project
     * @property project
     * @property mode
     * @property processName
     * @constructor Create empty Project info
     */
    data class ProjectInfo(
        val project: Project,
        val mode: Int = TaskScheduler.ALL_PROCESS_MODE,
        val processName: String
    )

    data class TaskInfo(val id: String, val path: String) {
        internal val predecessors = mutableListOf<String>()
        var threadPriority: Int = 0
        var taskPriority: Int = Task.DEFAULT_TASK_PRIORITY

        fun addPredecessors(predecessors: List<String>) {
            this.predecessors.addAll(predecessors)
        }

        fun isFirst(): Boolean = predecessors.isEmpty()

        override fun toString(): String {
            return "TaskInfo id: $id"
        }
    }

    data class TaskBundle(
        val mode: Int = TaskScheduler.ALL_PROCESS_MODE,
        val processName: String = "",
        val taskInfos: MutableList<TaskInfo> = mutableListOf()
    )
}