package com.hackx.ruraledtech.core.common

import android.util.Log
import com.hackx.ruraledtech.BuildConfig

/**
 * Logs by device/package/event id only. Never pass learner names, answers, or progress
 * through this — see PS section 21 (Security) and section 42/43.
 */
object SecureLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
