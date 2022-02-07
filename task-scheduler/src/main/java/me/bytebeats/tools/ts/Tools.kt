package me.bytebeats.tools.ts

import android.database.Cursor
import java.io.Closeable
import java.lang.Exception

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

