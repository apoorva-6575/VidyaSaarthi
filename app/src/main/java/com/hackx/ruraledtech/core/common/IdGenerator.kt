package com.hackx.ruraledtech.core.common

import java.util.UUID
import javax.inject.Inject

class IdGenerator @Inject constructor() {
    fun learnerId(): String = "L-${UUID.randomUUID()}"
    fun attemptId(): String = "A-${UUID.randomUUID()}"
    fun eventId(): String = "E-${UUID.randomUUID()}"
    fun deviceId(): String = "D-${UUID.randomUUID()}"
}
