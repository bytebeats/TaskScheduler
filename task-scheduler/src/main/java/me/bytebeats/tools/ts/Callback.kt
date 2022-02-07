package me.bytebeats.tools.ts

/**
 * Created by bytebeats on 2022/2/7 : 19:08
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * project lifecycle call
 * listen to lifecycle of project
 * Note: thread safety should be considered when callback is invoked.
 */
interface Callback {
    /**
     * On project start
     * start executing project
     * Note: onProjectStart() is invoked in Thread on which task is running
     */
    fun onProjectStart()

    /**
     * On project finish
     * finish executing project
     * Note: onProjectFinish() is invoked in Thread on which task is running
     */
    fun onProjectFinish()

    /**
     * On task finish
     *  when tasks of project is finished
     *  Note: onTaskFinish(String) is invoked in Thread on which task is running
     * @param taskName
     */
    fun onTaskFinish(taskName: String)
}