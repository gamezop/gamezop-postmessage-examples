package com.gamezop.postmessageexample.data

import com.gamezop.postmessageexample.model.CapturedGameEvent
import com.gamezop.postmessageexample.model.DisplayField
import com.gamezop.postmessageexample.model.EventFamily
import com.gamezop.postmessageexample.model.GameEventPayload
import com.gamezop.postmessageexample.model.ValidationStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GameEventParser {
    fun parse(id: Long, receivedAtEpochMillis: Long, rawJson: String): CapturedGameEvent {
        return try {
            val json = JSONObject(rawJson)
            when {
                json.has("state") -> parseIndividual(id, receivedAtEpochMillis, rawJson, json)
                json.has("event") -> parseBattles(id, receivedAtEpochMillis, rawJson, json)
                else -> CapturedGameEvent(
                    id = id,
                    receivedAtEpochMillis = receivedAtEpochMillis,
                    rawJson = rawJson,
                    prettyJson = json.toString(2),
                    family = EventFamily.UNKNOWN,
                    name = "unknown_payload",
                    payload = GameEventPayload.Unknown(json.toDisplayFields()),
                    status = ValidationStatus.UNKNOWN,
                )
            }
        } catch (error: JSONException) {
            val reason = error.message ?: "Payload is not a JSON object"
            CapturedGameEvent(
                id = id,
                receivedAtEpochMillis = receivedAtEpochMillis,
                rawJson = rawJson,
                prettyJson = rawJson,
                family = EventFamily.UNKNOWN,
                name = "malformed_json",
                payload = GameEventPayload.Malformed(reason),
                status = ValidationStatus.MALFORMED,
            )
        }
    }

    private fun parseIndividual(
        id: Long,
        receivedAtEpochMillis: Long,
        rawJson: String,
        json: JSONObject,
    ): CapturedGameEvent {
        val state = json.optString("state", "unknown")
        val known = state in INDIVIDUAL_STATES
        return CapturedGameEvent(
            id = id,
            receivedAtEpochMillis = receivedAtEpochMillis,
            rawJson = rawJson,
            prettyJson = json.toString(2),
            family = EventFamily.INDIVIDUAL,
            name = state,
            payload = GameEventPayload.Individual(
                state = state,
                gameCode = json.optionalString("gameCode"),
                gamePlayId = json.optionalString("gamePlayId"),
                score = json.optionalNumber("score"),
                duration = json.optionalNumber("duration")?.toLong(),
                displayFields = json.toDisplayFields(),
            ),
            status = if (known) ValidationStatus.VALID else ValidationStatus.UNKNOWN,
        )
    }

    private fun parseBattles(
        id: Long,
        receivedAtEpochMillis: Long,
        rawJson: String,
        json: JSONObject,
    ): CapturedGameEvent {
        val event = json.optString("event", "unknown")
        val known = event in BATTLES_EVENTS
        return CapturedGameEvent(
            id = id,
            receivedAtEpochMillis = receivedAtEpochMillis,
            rawJson = rawJson,
            prettyJson = json.toString(2),
            family = EventFamily.BATTLES,
            name = event,
            payload = GameEventPayload.Battles(
                event = event,
                gameCode = json.optionalString("gameCode"),
                matchId = json.optionalString("matchId"),
                displayFields = json.toDisplayFields(),
            ),
            status = if (known) ValidationStatus.VALID else ValidationStatus.UNKNOWN,
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun JSONObject.optionalNumber(key: String): Number? =
        if (has(key) && !isNull(key)) opt(key) as? Number else null

    private fun JSONObject.toDisplayFields(): List<DisplayField> {
        val names = keys().asSequence().toList().sorted()
        return names.map { key -> DisplayField(key, valueForDisplay(opt(key))) }
    }

    private fun valueForDisplay(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.toString(2)
        is JSONArray -> value.toString(2)
        else -> value.toString()
    }

    companion object {
        val INDIVIDUAL_STATES = setOf("loaded", "start", "playing", "over", "levelup")
        val BATTLES_EVENTS = setOf(
            "match_found",
            "match_not_found",
            "match_start",
            "match_playing",
            "match_over",
            "match_result",
            "go_home",
        )
    }
}
