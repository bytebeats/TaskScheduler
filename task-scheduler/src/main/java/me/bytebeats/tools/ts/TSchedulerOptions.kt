package me.bytebeats.tools.ts

import android.app.Application
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by bytebeats on 2022/2/7 : 15:00
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
interface TSchedulerOptions {
    /**
     * Log switch, by default true
     */
    var isLoggable: Boolean

    /**
     * Core number
     * the number of core thread, by default, the number of available processors
     */
    var coreNumber: Int

    /**
     * Thread factory
     * by default defaultThreadFactory() which generates Thread with name prefixed with "TaskScheduler Thread #count"
     */
    var threadFactory: ThreadFactory

    /**
     * Executor Service
     * by default corePoolSize is number of available processors, cache queue size has no limit.
     * When Thread is idle for more than 60s, it will be released.
     */
    var executor: ExecutorService

    /**
     * Task timeout
     * by default, 400ms
     * When task is running for more than ${timeout}ms, toast warning will shown, if enabled.
     */
    var taskTimeout: Long

    var isToastWarningEnabled: Boolean

    /**
     * Enable show toast warning
     *
     * @param context to create Toast object.
     * @param enabledToastWarning toast warning is enabled
     */
    fun enableShowToastWarning(context: Application, enabledToastWarning: Boolean)

    var context: Application?

    val toastWarningAvailable: Boolean

    companion object : TSchedulerOptions {
        private var sIsLoggable = true
        private var mCoreNumber: Int = Runtime.getRuntime().availableProcessors()
        private var mThreadFactory: ThreadFactory? = null
        private var mExecutor: ExecutorService? = null
        private var mTaskTimeout: Long = 400L
        private var sIsToastWarningEnabled: Boolean = false
        private var mContext: Application? = null

        //==============================================================================================
        // PUBLIC API
        //==============================================================================================

        /**
         * Log switch, by default true
         */
        override var isLoggable: Boolean
            get() = isLoggable
            set(value) {
                sIsLoggable = value
            }

        /**
         * Core number
         * the number of core thread, by default, the number of available processors
         */
        override var coreNumber: Int
            get() = mCoreNumber
            set(value) {
                mCoreNumber = value
            }

        /**
         * Thread factory
         * by default defaultThreadFactory() which generates Thread with name prefixed with "TaskScheduler Thread #count"
         */
        override var threadFactory: ThreadFactory
            get() {
                if (mThreadFactory == null)
                    mThreadFactory = defaultThreadFactory()
                return mThreadFactory!!
            }
            set(value) {
                mThreadFactory = value
            }

        /**
         * Executor Service
         * by default corePoolSize is number of available processors, cache queue size has no limit.
         * When Thread is idle for more than 60s, it will be released.
         */
        override var executor: ExecutorService
            get() {
                if (mExecutor == null) {
                    mExecutor = defaultExecutorService()
                }
                return mExecutor!!
            }
            set(value) {
                mExecutor = value
            }

        /**
         * Task timeout
         * by default, 400ms
         * When task is running for more than ${timeout}ms, toast warning will shown, if enabled.
         */
        override var taskTimeout: Long
            get() = mTaskTimeout
            set(value) {
                mTaskTimeout = value
            }

        override var isToastWarningEnabled: Boolean
            get() = sIsToastWarningEnabled
            set(value) {
                sIsToastWarningEnabled = value
            }

        /**
         * Enable show toast warning
         *
         * @param context to create Toast object.
         * @param enabledToastWarning toast warning is enabled
         */
        override fun enableShowToastWarning(context: Application, enabledToastWarning: Boolean) {
            mContext = context
            sIsToastWarningEnabled = enabledToastWarning
        }

        override val toastWarningAvailable: Boolean
            get() = isToastWarningEnabled && context != null

        override var context: Application?
            get() = mContext
            set(value) {
                mContext = value
            }

        //==============================================================================================
        // PRIVATE METHOD
        //==============================================================================================

        private fun defaultThreadFactory(): ThreadFactory =
            object : ThreadFactory {
                private val mThreadCounter = AtomicInteger(0)

                override fun newThread(r: Runnable?): Thread =
                    Thread(r, "TaskScheduler Thread # ${mThreadCounter.getAndIncrement()}")
            }

        private fun defaultExecutorService(): ExecutorService =
            ThreadPoolExecutor(
                mCoreNumber,
                mCoreNumber,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingDeque(),
                threadFactory
            ).apply {
                allowCoreThreadTimeOut(true)
            }
    }
}