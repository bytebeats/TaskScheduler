package me.bytebeats.tools.ts


/**
 * Created by bytebeats on 2022/2/7 : 18:55
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class Project private constructor(private var projectName: String = DEFAULT_NAME) :
    Task(projectName), Callback {
    private var mStartTask: Task? = null
    private var mFinishTask: AnchorTask? = null
    private val mCallbacks = mutableListOf<Callback>()
    private var mDurationMonitor: DurationMonitor? = null
    private var mDurationMonitorCallback: DurationMonitor.Callback? = null

    internal fun setStartTask(task: Task) {
        mStartTask = task
    }

    internal fun setFinishTask(anchorTask: AnchorTask) {
        mFinishTask = anchorTask
    }

    internal fun setDurationMonitor(monitor: DurationMonitor) {
        mDurationMonitor = monitor
    }

    fun setDurationMonitorCallback(callback: DurationMonitor.Callback) {
        mDurationMonitorCallback = callback
    }

    fun addProjectCallback(callback: Callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback)
        }
    }

    override fun start() {
        mStartTask?.start()
    }

    override fun addSuccessor(task: Task) {
        mFinishTask?.addSuccessor(task)
    }

    override fun currentState(): State {
        return when {
            State.IDLE == mStartTask?.currentState() -> State.IDLE
            State.FINISHED == mFinishTask?.currentState() -> State.FINISHED
            else -> State.RUNNING
        }
    }

    override fun isRunning(): Boolean = currentState() == State.RUNNING

    override fun isFinished(): Boolean = currentState() == State.FINISHED

    override fun addOnTaskFinishListener(listener: OnTaskFinishListener) {
        mFinishTask?.addOnTaskFinishListener(listener)
    }

    override fun recycle() {
        super.recycle()
        mCallbacks.clear()
    }

    override fun execute() {
        // do nothing here
    }

    override fun onProjectStart() {
        mDurationMonitor?.startRecordProject()
        mCallbacks.forEach { c -> c.onProjectStart() }
    }

    override fun onProjectFinish() {
        mDurationMonitor?.stopRecordProject()
        mCallbacks.forEach { c -> c.onProjectFinish() }
        mDurationMonitor?.let {
            mDurationMonitorCallback?.onProjectDurationInvoked(it.projectDuration)
            mDurationMonitorCallback?.onTaskDurationsInvoked(it.taskDurations())
        }
    }

    override fun onTaskFinish(taskName: String) {
        mCallbacks.forEach { c -> c.onTaskFinish(taskName) }
    }

    class Builder {
        private var mCacheTask: Task? = null
        private var mStartTask: AnchorTask? = null
        private var mFinishTask: AnchorTask? = null
        private var mProject: Project? = null
        private var mDurationMonitor: DurationMonitor? = null
        private var mTaskStore: TaskStore? = null
        private var sIsPositionSet: Boolean = true

        init {
            reset()
        }

        private fun reset() {
            mCacheTask = null
            sIsPositionSet = true
            mProject = Project()
            mFinishTask = AnchorTask("TaskScheduler-DefaultFinishTask", false)
            mFinishTask!!.projectCallback = mProject
            mStartTask = AnchorTask("TaskScheduler-DefaultStartTask", true)
            mStartTask!!.projectCallback = mProject
            mProject!!.setStartTask(mStartTask!!)
            mProject!!.setFinishTask(mFinishTask!!)
            mDurationMonitor = DurationMonitor()
            mProject!!.setDurationMonitor(mDurationMonitor!!)
            mTaskStore = TaskStore(TaskFactory.DEFAULT)
        }

        private fun addToRootIfNeed() {
            if (!sIsPositionSet && mCacheTask != null) {
                mStartTask?.addSuccessor(mCacheTask!!)
            }
        }

        fun withTaskFactory(factory: TaskFactory): Builder {
            mTaskStore = TaskStore(factory)
            return this
        }

        fun setOnProjectCallback(callback: Callback): Builder {
            mProject?.addProjectCallback(callback)
            return this
        }

        fun setOnDurationMonitorCallback(callback: DurationMonitor.Callback): Builder {
            mProject?.setDurationMonitorCallback(callback)
            return this
        }

        fun withName(projectName: String): Builder {
            mProject?.projectName = projectName
            return this
        }



        fun build(): Project {
            addToRootIfNeed()
            val project = mProject

            /**
             * After create project, reset configurations and be ready to create next one
             */
            reset()
            return project!!
        }
    }

    internal class AnchorTask(name: String, private val isAnchorTask: Boolean = true) : Task(name) {
        var projectCallback: Callback? = null

        override fun execute() {
            projectCallback?.let {
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