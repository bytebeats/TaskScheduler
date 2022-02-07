package me.bytebeats.tools.ts

import android.os.Handler
import android.os.Looper
import android.os.Process
import java.util.concurrent.ExecutorService

/**
 * Created by bytebeats on 2022/2/7 : 16:54
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */


/**
 * Task
 *
 * @property name task name
 * @property threadPriority the priority of the Thread on which task is running,
 *           the greater, the more cpu time.
 *           defined in {@link android.os.Process}，
 *           like {@link android.os.Process#THREAD_PRIORITY_DEFAULT},
 *           {@link android.os.Process#THREAD_PRIORITY_BACKGROUND},
 *           {@link android.os.Process#THREAD_PRIORITY_FOREGROUND}
 * @property isMainTask is Task running on Main Thread, by default false
 * @constructor Create  Task
 */
abstract class Task @JvmOverloads constructor(
    internal var name: String,
    internal var threadPriority: Int = Process.THREAD_PRIORITY_DEFAULT,
    internal var isMainTask: Boolean = false
) : Runnable {

    /**
     * task priority
     * the less, the more prior to execute
     * Because thread pool has limited Thread, running tasks has orders to run.
     */
    private var mTaskPriority: Int = DEFAULT_TASK_PRIORITY

    var taskPriority: Int
        get() = mTaskPriority
        set(value) {
            mTaskPriority = value
        }

    @Volatile
    private var mState = State.IDLE

    private val mTaskFinishListeners = mutableListOf<OnTaskFinishListener>()
    private val mTaskStartListeners = mutableListOf<OnTaskStartListener>()

    /**
     * successor tasks
     */
    private val mSuccessors = mutableListOf<Task>()

    /**
     *  predecessor tasks
     */
    private val mPredecessors = mutableSetOf<Task>()

    var durationMonitor: DurationMonitor? = null

    //==============================================================================================
    // PUBLIC METHOD
    //==============================================================================================

    /**
     * Current state of current Task
     *
     * @return
     */
    fun currentState(): State = mState

    /**
     * Is current Task running?
     *
     * @return true means current Task is running or false
     */
    fun isRunning(): Boolean = mState == State.RUNNING

    /**
     * Is current finished?
     *
     * @return true means current Task is finished or false.
     */
    fun isFinished(): Boolean = mState == State.FINISHED

    fun addOnTaskFinishListener(listener: OnTaskFinishListener) {
        if (mTaskFinishListeners.contains(listener))
            return
        mTaskFinishListeners.add(listener)
    }

    fun addOnTaskStartListener(listener: OnTaskStartListener) {
        if (mTaskStartListeners.contains(listener)) return
        mTaskStartListeners.add(listener)
    }

    @Throws(IllegalStateException::class)
    @Synchronized
    fun start() {
        if (mState != State.IDLE) {
            throw IllegalStateException("You try to run task $name twice, is there a circular dependency?")
        }
        switchState(State.WAIT)
        if (isMainTask) {
            mainTaskHandler.post(this)
        } else {
            taskExecutor.execute(this)
        }
    }

    override fun run() {
        Process.setThreadPriority(threadPriority)
        val startTimestamp = System.currentTimeMillis()
        switchState(State.RUNNING)
        execute()
        switchState(State.FINISHED)
        val finishTimestamp = System.currentTimeMillis()
        recordTime(finishTimestamp - startTimestamp)

        notifyFinished()
        recycle()
    }

    protected abstract fun execute()

    //==============================================================================================
    // INTERNAL METHOD
    //==============================================================================================

    internal fun notifyFinished() {
        if (mSuccessors.isNotEmpty()) {
            // TODO: sort mSuccessors
            mSuccessors.forEach { s -> s.onPredecessorFinished(this) }
        }
        mTaskFinishListeners.forEach { l -> l.onFinish(name) }
        mTaskFinishListeners.clear()
    }

    /**
     * Recycle
     * When current task is finished, release resources
     */
    internal fun recycle() {
        mSuccessors.clear()
        mTaskStartListeners.clear()
        mTaskFinishListeners.clear()
    }

    /**
     * On predecessor finished
     * one predecessor task of current task is finished.
     * If all predecessor task are finished, start current Task.
     * @param predecessor
     */
    @Synchronized
    internal fun onPredecessorFinished(predecessor: Task) {
        if (mPredecessors.isEmpty()) return
        mPredecessors.remove(predecessor)
        if (mPredecessors.isEmpty()) start()
    }

    /**
     * Add predecessor task of current Task
     *
     * @param task predecessor task
     */
    internal fun addPredecessor(task: Task) {
        mPredecessors.add(task)
    }

    /**
     * Remove predecessor task of current Task
     *
     * @param task predecessor task
     * @return true means predecessor Task is removed successfully or false
     */
    internal fun removePredecessor(task: Task): Boolean = mPredecessors.remove(task)

    /**
     * Add successor task of current Task
     *
     * @param task successor task
     */
    internal fun addSuccessor(task: Task) {
        if (task == this) {
            throw IllegalStateException("A task should not after itself.")
        }
        task.addPredecessor(this)
        mSuccessors.add(task)
    }

    /**
     * Remove successor task of current Task
     *
     * @param task successor task
     * @return true means successor is removed successfully or false
     */
    internal fun removeSuccessor(task: Task): Boolean =
        mSuccessors.remove(task) && task.removePredecessor(this)

    //==============================================================================================
    // PRIVATE METHOD
    //==============================================================================================

    private fun switchState(state: State) {
        mState = state
    }

    private fun recordTime(duration: Long) {
        durationMonitor?.recordTask(name, duration)
    }

    companion object {
        const val DEFAULT_TASK_PRIORITY = 0
        private val taskExecutor: ExecutorService
            get() = SchedulerOptions.executor
        private val mainTaskHandler: Handler
            get() = Handler(Looper.getMainLooper())
    }

    //==============================================================================================
    // INNER CLASSES
    //==============================================================================================

    /**
     * On task finish listener
     */
    interface OnTaskFinishListener {
        /**
         * On finish
         * invoked when task is finished
         *
         * @param taskName task's name
         */
        fun onFinish(taskName: String)
    }

    /**
     * On task start listener
     */
    interface OnTaskStartListener {
        /**
         * On start
         * invoked when task is started
         *
         * @param task
         */
        fun onStart(task: Task)
    }
}