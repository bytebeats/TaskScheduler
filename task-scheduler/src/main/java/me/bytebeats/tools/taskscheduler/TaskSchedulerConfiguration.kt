package me.bytebeats.tools.taskscheduler

/**
 * Created by bytebeats on 2022/2/7 : 15:00
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class TaskSchedulerConfiguration {
    companion object {
        private var sIsLoggable = true
        private var mCoreNumber: Int = Runtime.getRuntime().availableProcessors()

        var isLoggable: Boolean
            get() = isLoggable
            set(value) {
                sIsLoggable = value
            }
        var coreNumber: Int
            get() = mCoreNumber
            set(value) {
                mCoreNumber = value
            }
    }
}