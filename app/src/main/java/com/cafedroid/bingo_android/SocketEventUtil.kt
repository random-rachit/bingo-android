package com.cafedroid.bingo_android

import com.google.gson.annotations.SerializedName

abstract class ResponseEvent(@SerializedName("id") val eventId: String? = null)

class GameJoinEvent(@SerializedName("room") val room: GameRoom) : ResponseEvent()

class MemberUpdateEvent(
    @SerializedName("message") val message: String,
    @SerializedName("room") val room: GameRoom
) : ResponseEvent()