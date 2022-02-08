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

        /**
         * After
         * 指定紧前{@code Task}，必须这些{@code Task}执行完才能执行自己。如果不指定具体的紧前{@code Task}默认会最开始执行
         * @param task tasks to run before current task
         */
        fun after(task: Task): Builder {
            task.addSuccessor(mCacheTask!!)
            mFinishTask!!.removePredecessor(task)
            sIsPositionSet = true
            return this
        }

        /**
         * After
         * 指定紧前{@code Task}，必须这些{@code Task}执行完才能执行自己。如果不指定具体的紧前{@code Task}默认会最开始执行
         * @param tasks tasks to run before current task
         */
        fun after(vararg tasks: Task): Builder {
            for (task in tasks) {
                task.addSuccessor(mCacheTask!!)
                mFinishTask!!.removePredecessor(task)
            }
            sIsPositionSet = true
            return this
        }

        /**
         * After
         * 指定紧前{@code Task}，必须这些{@code Task}执行完才能执行自己。如果不指定具体的紧前{@code Task}默认会最开始执行
         * @param taskNames names of tasks to run before current task
         */
        fun after(vararg taskNames: String): Builder {
            after(*(taskNames.map { name -> mTaskStore!![name] }.toTypedArray()))
            return this
        }

        /**
         * 增加一个{@code Task}，在调用该方法后，需要调用{@link #after(Task)}来确定。
         * 它在图中的位置，如果不显式指定，则默认添加在最开始的位置。
         *
         * @param task 增加的{@code Task}对象.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        fun add(task: Task): Builder {
            addToRootIfNeed()
            mCacheTask = task
            mCacheTask!!.durationMonitor = mDurationMonitor
            sIsPositionSet = false
            mCacheTask!!.addOnTaskFinishListener(InnerOnTaskFinishListener(mProject))
            mCacheTask!!.addSuccessor(mFinishTask!!)
            return this
        }

        /**
         * 增加一个{@code Task}，在调用该方法后，需要调用{@link #after(Task)}来确定。
         * 它在图中的位置，如果不显式指定，则默认添加在最开始的位置。
         *
         * @param task 增加的{@code Task}对象.
         * @return {@code Builder}对象，可以继续添加属性或者组装{@code Task}。
         */
        fun add(taskName: String): Builder {
            val task = mTaskStore!![taskName]
            add(task)
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

    /**
     * <p>从图的执行角度来讲，应该要有唯一的开始位置和唯一的结束位置。这样就可以准确衡量一个图的开始和结束。并且可以
     * 通过开始点和结束点，方便地将这个图嵌入到另外一个图中去。</p>
     * <p>但是从用户的角度来理解，他可能会有多个{@code task}可以同时开始，也可以有多个{@code task}作为结束点。</p>
     * <p>为了解决这个矛盾，框架提供一个默认的开始节点和默认的结束节点。并且将这两个点称为这个{@code project}的锚点。
     * 用户添加的{@code task}都是添加在开始锚点后，用户的添加的{@code task}后也都会有一个默认的结束锚点。</p>
     * <p>如前面提到，锚点的作用有两个：
     * <li>标记一个{@code project}的开始和结束。</li>
     * <li>当{@code project}需要作为一个{@code task}嵌入到另外一个{@code project}里面时，锚点可以用来和其他{@code task}
     * 进行连接。</li>
     * </p>
     */
    internal class AnchorTask(name: String, private val isAnchorTask: Boolean = true) : Task(name) {
        var projectCallback: Callback? = null

        override fun execute() {
            projectCallback?.let {
                if (isAnchorTask) it.onProjectStart() else it.onProjectFinish()
            }
        }
    }

    private class InnerOnTaskFinishListener(private val project: Project?) :
        Task.OnTaskFinishListener {
        override fun onFinish(taskName: String) {
            project?.onTaskFinish(taskName)
        }
    }

    companion object {
        const val DEFAULT_NAME = "TaskSchedulerProject"
    }
}