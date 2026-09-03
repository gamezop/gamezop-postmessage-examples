package com.gamezop.postmessageexample.model

enum class GameMode(val label: String) {
    INDIVIDUAL("Individual"),
    BATTLES("Battles"),
}

enum class EventFamily {
    INDIVIDUAL,
    BATTLES,
    UNKNOWN,
}

enum class ValidationStatus {
    VALID,
    UNKNOWN,
    MALFORMED,
}

data class DisplayField(
    val name: String,
    val value: String,
)

sealed interface GameEventPayload {
    val displayFields: List<DisplayField>

    data class Individual(
        val state: String,
        val gameCode: String?,
        val gamePlayId: String?,
        val score: Number?,
        val duration: Long?,
        override val displayFields: List<DisplayField>,
    ) : GameEventPayload

    data class Battles(
        val event: String,
        val gameCode: String?,
        val matchId: String?,
        override val displayFields: List<DisplayField>,
    ) : GameEventPayload

    data class Unknown(
        override val displayFields: List<DisplayField>,
    ) : GameEventPayload

    data class Malformed(
        val reason: String,
        override val displayFields: List<DisplayField> = listOf(DisplayField("error", reason)),
    ) : GameEventPayload
}

data class CapturedGameEvent(
    val id: Long,
    val receivedAtEpochMillis: Long,
    val rawJson: String,
    val prettyJson: String,
    val family: EventFamily,
    val name: String,
    val payload: GameEventPayload,
    val status: ValidationStatus,
)

data class EventLogState(
    val events: List<CapturedGameEvent> = emptyList(),
    val selectedEventId: Long? = null,
    val droppedEventCount: Int = 0,
)
