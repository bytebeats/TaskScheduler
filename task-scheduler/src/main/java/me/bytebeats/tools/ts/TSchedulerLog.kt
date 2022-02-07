package me.bytebeats.tools.ts

import android.util.Log

/**
 * Created by bytebeats on 2022/2/7 : 15:58
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

typealias Options = TSchedulerOptions

interface TaskSchedulerLog {
    fun d(tag: String?, any: Any)
    fun d(tag: String?, formatter: String, vararg args: Any)
    fun w(tag: String?, any: Any)
    fun w(tag: String?, formatter: String, vararg args: Any)
    fun i(tag: String?, any: Any)
    fun e(tag: String?, e: Throwable)
    fun print(any: Any)
    fun print(formatter: String, vararg args: Any)


    companion object : TaskSchedulerLog {
        private const val TAG = "TaskScheduler"
        private fun getTag(tag: String?): String = tag ?: TAG
        override fun d(tag: String?, any: Any) {
            if (Options.isLoggable) {
                Log.d(getTag(tag), any.toString())
            }
        }

        override fun d(tag: String?, formatter: String, vararg args: Any) {
            if (Options.isLoggable) {
                Log.d(getTag(tag), formatter.format(args))
            }
        }

        override fun w(tag: String?, any: Any) {
            if (Options.isLoggable) {
                Log.w(getTag(tag), any.toString())
            }
        }

        override fun w(tag: String?, formatter: String, vararg args: Any) {
            if (Options.isLoggable) {
                Log.w(getTag(tag), formatter.format(args))
            }
        }

        override fun i(tag: String?, any: Any) {
            if (Options.isLoggable) {
                Log.i(getTag(tag), any.toString())
            }
        }

        override fun e(tag: String?, e: Throwable) {
            if (Options.isLoggable) {
                Log.e(getTag(tag), e.stackTraceToString())
            }
        }

        override fun print(any: Any) {
            d(TAG, any)
        }

        override fun print(formatter: String, vararg args: Any) {
            d(TAG, formatter, args)
        }
    }
}