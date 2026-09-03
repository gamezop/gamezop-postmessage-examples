package com.gamezop.postmessageexample.data

import androidx.lifecycle.ViewModel
import com.gamezop.postmessageexample.model.EventLogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameEventViewModel(
    private val parser: GameEventParser = GameEventParser(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(EventLogState())
    val state: StateFlow<EventLogState> = mutableState.asStateFlow()

    private var nextEventId = 1L

    fun capture(rawJson: String) {
        val captured = parser.parse(nextEventId++, clock(), rawJson)
        mutableState.update { current ->
            val appended = current.events + captured
            val overflow = (appended.size - MAX_EVENTS).coerceAtLeast(0)
            current.copy(
                events = if (overflow == 0) appended else appended.drop(overflow),
                selectedEventId = captured.id,
                droppedEventCount = current.droppedEventCount + overflow,
            )
        }
    }

    fun select(eventId: Long) {
        mutableState.update { it.copy(selectedEventId = eventId) }
    }

    fun clear() {
        mutableState.value = EventLogState()
    }

    companion object {
        const val MAX_EVENTS = 500
    }
}

