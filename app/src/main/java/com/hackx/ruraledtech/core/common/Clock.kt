package com.hackx.ruraledtech.core.common

import javax.inject.Inject

interface AppClock {
    fun nowMillis(): Long
}

class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
