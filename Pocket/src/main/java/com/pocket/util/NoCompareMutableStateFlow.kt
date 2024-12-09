package com.pocket.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

interface NoCompareStateFlow<T> : SharedFlow<T> {
    val value: T
}

/**
 * Like a [MutableStateFlow], but will always emit a set value, even if the new value equals the old one.
 */
class NoCompareMutableStateFlow<T>(
    value: T
) : NoCompareStateFlow<T> {

    override var value: T = value
        set(value) {
            field = value
            innerFlow.tryEmit(value)
        }

    private val innerFlow = MutableSharedFlow<T>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        innerFlow.tryEmit(value)
    }

    override suspend fun collect(collector: FlowCollector<T>): Nothing = innerFlow.collect(collector)
    override val replayCache: List<T> = innerFlow.replayCache
}

/**
 * Updates the [NoCompareMutableStateFlow.value] atomically using the specified [function] of its value.
 *
 * [function] may be evaluated multiple times, if [NoCompareMutableStateFlow.value] is being concurrently updated.
 */
// Note:
// This is trivial, because it replaced a trivial [NoCompareMutableStateFlow].compareAndSet
// implementation and is kept just for compatibility with [MutableStateFlow] API.
// But this means this implementation doesn't have the same safety and guarantees when
// updating concurrently from multiple threads.
inline fun <T> NoCompareMutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}
