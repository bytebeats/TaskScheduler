package me.bytebeats.tools.ts

import android.content.Context
import android.util.SparseArray
import java.io.*

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
     * finished task's names
     */
    private val mFinishedTasks = mutableListOf<String>()

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

    /**
     * 通过配置文件来设置启动流程。
     * TaskScheduler configuration file can be xml, json, yaml or properties file.
     * By default, xml file is supported by {@link XmlTaskParser}
     * JsonTaskParser/YamlTaskParser/PropertyTaskParser can be implemented by free mind
     * @param `in` 文件输入流
     */
    fun addProjectFrom(`in`: InputStream) {
        val projectInfos = SchedulerOptions.taskParser.parse(`in`)
        if (projectInfos.isEmpty()) {
            throw RuntimeException("Failed in parsing xml configuration file.")
        }
        for (info in projectInfos) {
            if (info.processName.isEmpty()) {
                addProject(info.project, info.mode)
            } else {
                addProject(info.project, info.processName)
            }
        }
    }

    fun addProjectFrom(file: File) {
        if (!file.exists()) {
            throw FileNotFoundException("TaskScheduler configuration file is not existed")
        }
        var ins: InputStream? = null
        try {
            ins = FileInputStream(file)
            addProjectFrom(ins)
        } catch (ioe: IOException) {
            SchedulerLog.e(SchedulerLog.TAG, ioe)
        } finally {
            ins.closeSafely()
        }
    }

    fun addProjectFrom(configurationPath: String) {
        addProjectFrom(File(configurationPath))
    }

    /**
     * Is startup finished
     * 判断当前进程的启动流程是否执行完成
     * @return true project execution is finished, false otherwise
     */
    fun isStartupFinished(): Boolean = sIsStartupFinished

    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            在启动结束时执行的{@code task}
     * @param mode            启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *                        {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     * @param taskPriority Linux线程优先级，从-19到20，-19优先级最高，20最低。
     */
    @JvmOverloads
    fun executeAfterStartup(
        task: Task,
        mode: Int = TaskScheduler.ALL_PROCESS_MODE,
        taskPriority: Int = Task.DEFAULT_TASK_PRIORITY
    ) {
        if (!context.isMatchMode(mode)) {
            return
        }
        if (isStartupFinished()) {
            task.start()
        } else {
            task.taskPriority = taskPriority
            addProjectBindTask(task)
        }
    }

    /**
     * <p>配置在启动完成时自动执行的{@code task}。</p>
     * <p>如果当前启动已经完成，该{@code task}会立即执行。如果当前启动暂未完成，则先会把该{@code task}缓存起来，
     * 待启动完成后根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code processName}判断当前的进程是否是该进程，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            在启动结束时执行的{@code task}。
     * @param processName     进程名，只有当前进程符合该进程名，才会执行{@code task}。
     * @param taskPriority    Linux线程优先级，从-19到20，-19优先级最高，20最低。
     */
    @JvmOverloads
    fun executeAfterStartup(
        task: Task,
        processName: String,
        taskPriority: Int = Task.DEFAULT_TASK_PRIORITY
    ) {
        if (!context.isMatchProcess(processName)) {
            return
        }
        if (isStartupFinished()) {
            task.start()
        } else {
            task.taskPriority = taskPriority
            addProjectBindTask(task)
        }
    }

    /**
     * <p>配置在某个名称为{@code taskName}的{@code task}执行结束时，自动执行参数中传入的{@code task}。</p>
     * <p>如果当前名为{@code taskName}的{@code task}已经执行完成，则直接执行传入的{@code task}，否则先缓存，
     * 待指定{@code task}执行完成后再根据执行优先级执行。</p>
     * <p><strong>注意：</strong>此函数会根据{@code mode}判断当前的进程是否符合{@code mode}，
     * 只有符合才会执行该{@code task}。</p>
     *
     * @param task            需要执行的{@code task}，在这里配置而不在{@code Project}中是因为他本身不属于{@code Project}，
     *                        只不过想尽早执行而已。
     * @param taskName        等待该{@code task}执行完成，执行参数中的{@code task}
     * @param mode            启动流程的执行模型。对应{@link AlphaManager#MAIN_PROCESS_MODE},
     *                        {@link AlphaManager#ALL_PROCESS_MODE}, {@link AlphaManager#SECONDARY_PROCESS_MODE}
     * @param taskPriority    Linux线程优先级，从-19到20，-19优先级最高，20最低。
     */
    @JvmOverloads
    fun executeAfterTask(
        task: Task,
        taskName: String,
        mode: Int = TaskScheduler.ALL_PROCESS_MODE,
        taskPriority: Int = Task.DEFAULT_TASK_PRIORITY
    ) {
        if (!context.isMatchMode(mode)) {
            return
        }
        synchronized(mTaskGraphLock) {
            if (isStartupFinished() || mFinishedTasks.contains(taskName)) {
                task.start()
            } else {
                task.taskPriority = taskPriority
                mTaskGraph[taskName] = task
            }
        }
    }

    @JvmOverloads
    fun executeAfterTask(
        task: Task,
        taskName: String,
        processName: String,
        taskPriority: Int = Task.DEFAULT_TASK_PRIORITY
    ) {
        if (!context.isMatchProcess(processName)) {
            return
        }
        synchronized(mTaskGraphLock) {
            if (isStartupFinished() || mFinishedTasks.contains(taskName)) {
                task.start()
            } else {
                task.taskPriority = taskPriority
                mTaskGraph[taskName] = task
            }
        }
    }

    private fun addProjectBindTask(task: Task) {
        synchronized(mTaskQueueLock) {
            mTaskQueue.add(task)
        }
    }

    /**
     * <p>阻塞当前线程，直到初始化任务完成。</p>
     * <p><strong>注意：如果你在执行task的线程上调用该函数，则存在死锁的风险。</strong></p>
     * <p>例如: <br>
     * 有一个{@code task}在线程A中执行，然后在该线程中调用这个函数，则可能导致死锁。因为此处block需要任务执行
     * 完才能release，而任务又需要在线程A执行。所以应该确保不在执行{@code task}的线程中调用该函数。</p>
     */
    fun waitUntilFinish() {
        synchronized(mWaitToFinishLock) {
            while (!sIsStartupFinished) {
                try {
                    mWaitToFinishLock.wait()
                } catch (ie: InterruptedException) {
                    SchedulerLog.e(SchedulerLog.TAG, ie)
                }
            }
        }
    }

    fun waitUntilFinish(timeout: Long): Boolean {
        val start = System.currentTimeMillis()
        var timeToWait = 0L
        synchronized(mWaitToFinishLock) {
            while (!sIsStartupFinished && timeToWait < timeout) {
                try {
                    mWaitToFinishLock.wait(timeout)
                } catch (ie: InterruptedException) {
                    SchedulerLog.e(SchedulerLog.TAG, ie)
                }
                timeToWait = System.currentTimeMillis() - start
            }
        }
        return timeToWait > timeout
    }

    private fun addListeners(project: Project) {
        project.addOnTaskFinishListener(object : Task.OnTaskFinishListener {
            override fun onFinish(taskName: String) {
                sIsStartupFinished = true
                recycle()
                releaseWaitToFinishLock()
            }
        })
    }

    private fun releaseWaitToFinishLock() {
        synchronized(mWaitToFinishLock) {
            mWaitToFinishLock.notifyAll()
        }
    }

    /**
     * 当启动流程完成，相应的资源应该及时释放。
     */
    private fun recycle() {
        mCurrentProcessProject = null
        mSparseProjectArray.clear()
    }

    private fun executeProjectBindRunnable(taskName: String) {
        val tasks = mTaskGraph.getValue(taskName)
        tasks?.sort()
        if (tasks != null) {
            for (task in tasks) {
                task.start()
            }
        }
        mTaskGraph.removeList(taskName)
    }

    private fun executeProjectBindRunnables() {
        mTaskQueue.sort()
        for (task in mTaskQueue) {
            task.start()
        }
        mTaskQueue.clear()
    }


    private inner class ProjectListener : OnProjectListener {
        override fun onProjectStart() {
            // do nothing here
        }

        override fun onProjectFinish() {
            synchronized(mTaskQueueLock) {
                if (mTaskQueue.isNotEmpty()) {
                    executeProjectBindRunnables()
                }
            }

            synchronized(mTaskGraphLock) {
                mFinishedTasks.clear()
            }
        }

        override fun onTaskFinish(taskName: String) {
            synchronized(mTaskGraphLock) {
                mFinishedTasks.add(taskName)
                if (mTaskGraph.containsKey(taskName)) {
                    executeProjectBindRunnable(taskName)
                }
            }
        }
    }

    companion object {
        const val MAIN_PROCESS_MODE = 0x00000001
        const val SECONDARY_PROCESS_MODE = 0x00000002
        const val ALL_PROCESS_MODE = 0x00000003

        private val mTaskQueueLock = byteArrayOf()
        private val mTaskGraphLock = byteArrayOf()
        private val mWaitToFinishLock = Object()

        @Volatile
        private var mInstance: TaskScheduler? = null

        fun getInstance(context: Context): TaskScheduler {
            return mInstance ?: synchronized(this) {
                mInstance ?: TaskScheduler(context.applicationContext).also { mInstance = it }
            }
        }
    }
}