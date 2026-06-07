package com.yuquewatch.data.source

import kotlinx.coroutines.flow.MutableSharedFlow

/** Emits a short quota string on each official (api/v2) call, for the debug toast. */
object ApiQuota {
    val events = MutableSharedFlow<String>(extraBufferCapacity = 16)
}
