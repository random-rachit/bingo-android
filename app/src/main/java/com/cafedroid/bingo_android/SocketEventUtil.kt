package com.cafedroid.bingo_android

import com.google.gson.annotations.SerializedName

object LocalEvents {
    const val NUMBER_STACK_CHANGE_EVENT = "100"
}

abstract class ResponseEvent(@SerializedName("id") val eventId: String? = null)

class GameJoinEvent(@SerializedName("room") val room: GameRoom) : ResponseEvent()

class MemberUpdateEvent(
    @SerializedName("message") val message: String,
    @SerializedName("room") val room: GameRoom
) : ResponseEvent()

class BingoNumberUpdateEvent(
    @SerializedName("user") val user: String,
    @SerializedName("number") val number: Int,
    @SerializedName("hasWon") val hasWon: Boolean = false
): ResponseEvent()

class NumberStackChangeEvent : ResponseEvent(LocalEvents.NUMBER_STACK_CHANGE_EVENT)