package com.gamezop.postmessageexample.data

import com.gamezop.postmessageexample.model.EventFamily
import com.gamezop.postmessageexample.model.GameEventPayload
import com.gamezop.postmessageexample.model.ValidationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEventParserTest {
    private val parser = GameEventParser()

    @Test
    fun `parses every documented individual state`() {
        GameEventParser.INDIVIDUAL_STATES.forEachIndexed { index, state ->
            val event = parser.parse(
                id = index.toLong(),
                receivedAtEpochMillis = 100L,
                rawJson = """{"state":"$state","gameCode":"demo","score":10,"duration":20}""",
            )

            assertEquals(EventFamily.INDIVIDUAL, event.family)
            assertEquals(state, event.name)
            assertEquals(ValidationStatus.VALID, event.status)
            assertTrue(event.payload is GameEventPayload.Individual)
        }
    }

    @Test
    fun `parses every documented battles event`() {
        GameEventParser.BATTLES_EVENTS.forEachIndexed { index, name ->
            val event = parser.parse(
                id = index.toLong(),
                receivedAtEpochMillis = 100L,
                rawJson = """{"event":"$name","gameCode":"demo","players":[]}""",
            )

            assertEquals(EventFamily.BATTLES, event.family)
            assertEquals(name, event.name)
            assertEquals(ValidationStatus.VALID, event.status)
            assertTrue(event.payload is GameEventPayload.Battles)
        }
    }

    @Test
    fun `preserves optional and extra fields`() {
        val event = parser.parse(
            id = 1,
            receivedAtEpochMillis = 100L,
            rawJson = """{"state":"playing","futureField":{"enabled":true}}""",
        )

        val payload = event.payload as GameEventPayload.Individual
        assertEquals(null, payload.gameCode)
        assertTrue(payload.displayFields.any { it.name == "futureField" && "enabled" in it.value })
        assertTrue("futureField" in event.prettyJson)
    }

    @Test
    fun `unknown state is visible instead of rejected`() {
        val event = parser.parse(1, 100L, """{"state":"bonus_round"}""")

        assertEquals(EventFamily.INDIVIDUAL, event.family)
        assertEquals(ValidationStatus.UNKNOWN, event.status)
        assertEquals("bonus_round", event.name)
    }

    @Test
    fun `payload without discriminator is unknown`() {
        val event = parser.parse(1, 100L, """{"message":"hello"}""")

        assertEquals(EventFamily.UNKNOWN, event.family)
        assertEquals(ValidationStatus.UNKNOWN, event.status)
        assertTrue(event.payload is GameEventPayload.Unknown)
    }

    @Test
    fun `malformed payload is isolated`() {
        val event = parser.parse(1, 100L, "{not-json")

        assertEquals(ValidationStatus.MALFORMED, event.status)
        assertEquals("malformed_json", event.name)
        assertEquals("{not-json", event.rawJson)
        assertTrue(event.payload is GameEventPayload.Malformed)
    }
}
