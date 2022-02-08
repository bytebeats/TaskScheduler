package me.bytebeats.tools.ts

import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Created by bytebeats on 2022/2/7 : 18:08
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Task duration recorder
 *
 * A class to record execution duration of the whole project and its all tasks
 */
class DurationMonitor {
    private val mTaskDurations = mutableMapOf<String, Long>()
    private var mProjectStartTime: Long = 0L
    private var mProjectDuration: Long = 0L

    private val mWarningHandler: Handler
        get() = Handler(Looper.getMainLooper())

    /**
     * Record task's duration in millisecond
     *
     * @param taskName
     * @param duration
     */
    @Synchronized
    fun recordTask(taskName: String, duration: Long) {
        SchedulerLog.d(
            "Executing task %s cost %s ms in thread: %s",
            taskName,
            duration,
            Thread.currentThread().name
        )
        if (duration >= SchedulerOptions.taskTimeout) {
            toastToWarn("Task %s run too long, cost %s ms", taskName, duration)
        }
        mTaskDurations[taskName] = duration
    }

    /**
     * Task executions
     * All finished tasks' durations
     * @return
     */
    @Synchronized
    fun taskDurations(): Map<String, Long> = mTaskDurations

    /**
     * Start record project execution
     *
     */
    fun startRecordProject() {
        mProjectStartTime = System.currentTimeMillis()
    }

    /**
     * Stop record project execution
     *
     */
    fun stopRecordProject() {
        mProjectDuration = System.currentTimeMillis() - mProjectStartTime
        SchedulerLog.d("project execution costs %s ms", mProjectDuration)
    }

    /**
     * Project duration
     */
    val projectDuration: Long
        get() = mProjectDuration

    /**
     * Toast to warn
     * Warn user thou Toast
     * @param formatter message formatter
     * @param args args to format
     */
    private fun toastToWarn(formatter: String, vararg args: Any) {
        if (SchedulerOptions.toastWarningAvailable) {
            mWarningHandler.post {
                SchedulerOptions.context?.let {
                    Toast.makeText(
                        it,
                        formatter.format(args),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * DurationMonitor Callback
     *
     */
    interface OnMonitorListener {
        /**
         * On task durations invoked
         * When DurationMonitor#taskDurations() is invoked
         * @param durations
         */
        fun onTaskDurationsInvoked(durations: Map<String, Long>)

        /**
         * On project duration invoked
         * When DurationMonitor#projectDuration is invoked
         * @param duration
         */
        fun onProjectDurationInvoked(duration: Long)
    }
}