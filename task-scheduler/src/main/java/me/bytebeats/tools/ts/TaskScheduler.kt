package me.bytebeats.tools.ts

import android.content.Context
import android.util.SparseArray
import java.io.InputStream

/**
 * Created by bytebeats on 2022/2/8 : 10:33
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

class TaskScheduler private constructor(private val context: Context) {

    /**
     * project for current process
     */
    private var mCurrentProcessProject: Task? = null

    /**
     * sparse project array
     * start projects for multiple launch mode
     */
    private val mSparseProjectArray = SparseArray<Task>()

    /**
     * is startup finished
     */
    @Volatile
    private var sIsStartupFinished = false

    private val mProjectListener = ProjectListener()

    /**
     * finished tasks
     */
    private val mFinishedTasks = mutableListOf<Task>()

    /**
     * task dependency graph
     * task and its predecessor tasks
     * {@code key} is task name, {@code value} is task's predecessor tasks
     */
    private val mTaskGraph = HashListMap<String, Task>()

    /**
     * task queue to run in order
     */
    private val mTaskQueue = mutableListOf<Task>()

    /**
     * 开始启动流程，这里会根据当前的进程执行合适的启动流程。挑选的过程如下：<br>
     * 1.检查是否有为当前进程设置单独的启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 2.检查当前是否主进程，且是否有为主进程配置启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 3.检查当前是否是非主进程，且是否有为非主进程配置启动流程，若有，则选择结束，执行启动流程。否则转入下一步；<br>
     * 4.检查是否有配置适合所有进程的启动流程，若有，则选择结束，执行启动流程。否则选择失败，没有启动流程可以执行。<br>
     */
    fun start() {
        val project: Project? = when {
            //1.是否有为当前进程单独配置的Project，此为最高优先级
            mCurrentProcessProject != null -> (mCurrentProcessProject as Project)
            //2.如果当前是主进程，是否有配置主进程Project
            context.isInMainProcess() && mSparseProjectArray.indexOfKey(MAIN_PROCESS_MODE) >= 0 ->
                (mSparseProjectArray.get(MAIN_PROCESS_MODE) as Project)
            //3.如果是非主进程，是否有配置非主进程的Project
            !context.isInMainProcess() && mSparseProjectArray.indexOfKey(SECONDARY_PROCESS_MODE) >= 0 ->
                (mSparseProjectArray[SECONDARY_PROCESS_MODE] as Project)
            //4.是否有配置适用所有进程的Project
            mSparseProjectArray.indexOfKey(ALL_PROCESS_MODE) >= 0 ->
                (mSparseProjectArray[ALL_PROCESS_MODE] as Project)
            else -> null
        }
        if (project != null) {

            project.start()
        } else {
            SchedulerLog.e(
                SchedulerLog.TAG,
                IllegalStateException("No startup project for current process.")
            )
        }
    }

    /**
     * Add project with mode
     *
     * @param project
     * @param mode like: <br>
     *                {@link #ALL_PROCESS_MODE}<br>
     *                {@link #MAIN_PROCESS_MODE}<br>
     *                {@link #SECONDARY_PROCESS_MODE}<br>
     *                by default mode is ALL_PROCESS_MODE which means
     * 增加一个启动流程，默认适合所有的进程，即{@link #ALL_PROCESS_MODE}模式。由于其范围最广，其优先级也是最低的。
     * 如果后面设置了针对具体进程的启动流程，则启动对应具体进程时，会优先执行该进程对应的启动流程。
     */
    @Throws(IllegalArgumentException::class)
    @JvmOverloads
    fun addProject(project: Task?, mode: Int = ALL_PROCESS_MODE) {
        if (project == null) {
            throw IllegalArgumentException("Project can't be null")
        }
        if (mode !in MAIN_PROCESS_MODE..ALL_PROCESS_MODE) {
            throw IllegalArgumentException("No such mode: $mode")
        }
        if (context.isMatchMode(mode)) {
            mSparseProjectArray.put(mode, project)
        }
    }

    /**
     * Add project
     *
     * add project for process with name {@param processName}
     * @param project
     * @param processName
     */
    fun addProject(project: Task?, processName: String) {
        if (context.isMatchProcess(processName)) {
            mCurrentProcessProject = project
        }
    }

    fun addProjectWithXml(xmlIS: InputStream) {
        val projectInfos : List<Any> ?= null

    }


    private class ProjectListener : OnProjectListener {
        override fun onProjectStart() {
            // do nothing here
        }

        override fun onProjectFinish() {
        }

        override fun onTaskFinish(taskName: String) {
        }
    }

    companion object {
        const val MAIN_PROCESS_MODE = 0x00000001
        const val SECONDARY_PROCESS_MODE = 0x00000002
        const val ALL_PROCESS_MODE = 0x00000003

        private val mTaskQueueLock = byteArrayOf()
        private val mTaskGraphLock = byteArrayOf()
        private val mWaitToFinishLock = byteArrayOf()

        @Volatile
        private var mInstance: TaskScheduler? = null

        fun getInstance(context: Context): TaskScheduler {
            return mInstance ?: synchronized(this) {
                mInstance ?: TaskScheduler(context.applicationContext).also { mInstance = it }
            }
        }
    }
}