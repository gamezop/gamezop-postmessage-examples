package com.gamezop.postmessageexample.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameEventViewModelTest {
    @Test
    fun `events remain ordered and newest event is selected`() {
        var time = 10L
        val viewModel = GameEventViewModel(clock = { time++ })

        viewModel.capture("""{"state":"loaded"}""")
        viewModel.capture("""{"state":"start"}""")

        val state = viewModel.state.value
        assertEquals(listOf("loaded", "start"), state.events.map { it.name })
        assertEquals(2L, state.selectedEventId)
        assertEquals(listOf(10L, 11L), state.events.map { it.receivedAtEpochMillis })
    }

    @Test
    fun `log keeps newest five hundred events`() {
        val viewModel = GameEventViewModel(clock = { 100L })

        repeat(GameEventViewModel.MAX_EVENTS + 4) { index ->
            viewModel.capture("""{"state":"playing","score":$index}""")
        }

        val state = viewModel.state.value
        assertEquals(GameEventViewModel.MAX_EVENTS, state.events.size)
        assertEquals(5L, state.events.first().id)
        assertEquals(4, state.droppedEventCount)
    }

    @Test
    fun `clear removes session data`() {
        val viewModel = GameEventViewModel(clock = { 100L })
        viewModel.capture("""{"event":"go_home"}""")

        viewModel.clear()

        assertEquals(emptyList<Any>(), viewModel.state.value.events)
        assertNull(viewModel.state.value.selectedEventId)
        assertEquals(0, viewModel.state.value.droppedEventCount)
    }
}

