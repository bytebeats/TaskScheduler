package me.bytebeats.tools.ts


/**
 * Created by bytebeats on 2022/2/7 : 18:55
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class Project : Task(DEFAULT_NAME), Callback {

    override fun execute() {

    }

    override fun onProjectStart() {
        TODO("Not yet implemented")
    }

    override fun onProjectFinish() {
        TODO("Not yet implemented")
    }

    override fun onTaskFinish(taskName: String) {
        TODO("Not yet implemented")
    }

    internal class AnchorTask(name: String, private val isAnchorTask: Boolean = true) : Task(name) {
        var projectListener: Callback? = null

        override fun execute() {
            projectListener?.let {
                if (isAnchorTask) it.onProjectStart() else it.onProjectFinish()
            }
        }
    }

    private class InnerOnTaskFinishListener(private val project: Project?) :
        OnTaskFinishListener {
        override fun onFinish(taskName: String) {
            project?.onTaskFinish(taskName)
        }
    }

    companion object {
        const val DEFAULT_NAME = "TaskSchedulerProject"
    }
}