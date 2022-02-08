package me.bytebeats.tools.ts

import android.app.ActivityManager
import android.content.Context
import android.database.Cursor
import android.os.Process
import java.io.Closeable
import java.io.FileInputStream
import kotlin.Exception

/**
 * Created by bytebeats on 2022/2/7 : 16:29
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

private val taskComparator = Comparator<Task> { t0, t1 -> t0.taskPriority - t1.taskPriority }

internal fun List<Task>.sort() {
    this.sortedWith(taskComparator)
}

/**
 * Close safely
 * Close a Closeable object safely
 * @return
 */
internal fun Closeable?.closeSafely(): Boolean {
    return if (this == null)
        false
    else
        try {
            this.close()
            true
        } catch (e: Exception) {
            TSchedulerLog.e(TSchedulerLog.TAG, e)
            false
        }
}

/**
 * Close safely
 * Close a Cursor object safely
 * {@link Cursor} is not a {@link Closeable} until 4.1.1, so we should supply this method to
 * close {@link Cursor} beside {@link #closeSafely(Closeable)}
 * @return
 */
internal fun Cursor?.closeSafely(): Boolean {
    return if (this == null)
        false
    else
        try {
            this.close()
            true
        } catch (e: Exception) {
            TSchedulerLog.e(TSchedulerLog.TAG, e)
            false
        }
}

private var mProcessName: String? = null

/**
 * Current process name
 *
 * @return the name of the current project.
 */
internal fun Context?.currentProcessName(): String? {
    var pName = currentProcessNameFromLinuxFile()
    if (pName.isNullOrEmpty() && this != null) {
        pName = currentProcessNameFromActivityManager()
    }
    return pName
}

internal fun currentProcessNameFromLinuxFile(): String? {
    val pid = Process.myPid()
    val line = "/proc/$pid/cmdline"
    var fis: FileInputStream? = null
    var processName: String? = null
    val buffer = ByteArray(1024)
    var read = 0
    try {
        fis = FileInputStream(line)
        read = fis.read(buffer)
    } catch (e: Exception) {
        TSchedulerLog.e(TSchedulerLog.TAG, e)
    } finally {
        fis?.closeSafely()
    }
    if (read > 0) {
        processName = String(buffer).trim()
    }
    return processName
}

internal fun Context?.currentProcessNameFromActivityManager(): String? {
    if (this == null) return null
    if (mProcessName != null) return mProcessName
    val pid = Process.myPid()
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    if (am == null) return null
    val pInfos = am.runningAppProcesses
    if (pInfos == null) return null
    for (pInfo in pInfos) {
        if (pid == pInfo?.pid) {
            mProcessName = pInfo.processName
            break
        }
    }
    return mProcessName
}

/**
 * Is in main process
 *
 * @return true if current process is main process, false otherwise
 */
internal fun Context.isInMainProcess(): Boolean {
    val mainProcessName = packageName
    val currentProcessName = currentProcessName()
    return !mainProcessName.isNullOrEmpty() && mainProcessName == currentProcessName
}

/**
 * Is match process
 * invoked in {@link TaskScheduler#addProject(Task, String)}
 * 只有进程名和当前进程相同，才有必要去持有该{@code Project}.
 * @param processName the name of the process to launch
 * @return whether current process name matches processName
 */
internal fun Context.isMatchProcess(processName: String?): Boolean {
    val curProcessName = currentProcessName()
    return processName == curProcessName
}

/**
 * Is match mode
 * invoked in {@link TaskScheduler#addProject(Task, String)}, 只有当前进程命中指定的{@code mode}，才有必要去持有该{@code
 * Project}.
 * @param mode the mode to launch Project
 * @return whether current process match this mode
 */
internal fun Context.isMatchMode(mode: Int): Boolean {
    return mode == TaskScheduler.ALL_PROCESS_MODE
            || isInMainProcess() && mode == TaskScheduler.MAIN_PROCESS_MODE
            || !isInMainProcess() && mode == TaskScheduler.SECONDARY_PROCESS_MODE
}

