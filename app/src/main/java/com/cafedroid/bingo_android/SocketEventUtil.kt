package com.cafedroid.bingo_android

import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

object LocalEvents {
    const val NUMBER_STACK_CHANGE_EVENT = "100"
}

const val GAME_JOIN_EVENT = 1001
const val MEMBER_UPDATE_EVENT = 1003
const val TURN_UPDATE_EVENT = 1006
const val GAME_STATE_CHANGE_EVENT = 1005

private val socketEventListener: Emitter.Listener = Emitter.Listener {
    Log.d("Utils", "${it[0]}")
    when (val eventId = (it[0] as JSONObject)[ApiConstants.ID]) {
        GAME_JOIN_EVENT -> {
            val gameJoinEvent = Gson().fromJson(it[0].toString(), GameJoinEvent::class.java)
            ActiveGameRoom.activeRoom = gameJoinEvent.room
            EventBus.getDefault().post(gameJoinEvent)
        }
        MEMBER_UPDATE_EVENT -> {
            val roomUpdateEvent = Gson().fromJson(it[0].toString(), MemberUpdateEvent::class.java)
            ActiveGameRoom.activeRoom = roomUpdateEvent.room
            EventBus.getDefault().post(roomUpdateEvent)
        }
        TURN_UPDATE_EVENT -> {
            val turnEvent = Gson().fromJson(it[0].toString(), TurnUpdateEvent::class.java)
            ActiveGameRoom.activeRoom?.userTurn = turnEvent.turn
            EventBus.getDefault().post(turnEvent)
        }
        GAME_STATE_CHANGE_EVENT -> {
            val gameState = Gson().fromJson(it[0].toString(), GameStateChangeEvent::class.java)
            ActiveGameRoom.activeRoom?.roomState = gameState.state
            EventBus.getDefault().post(gameState)
        }
        else -> {
            Log.e("SocketEventListener", "$eventId not handled")
        }
    }
}

object BingoSocket {
    var socket: Socket? = null

    fun connect() {
        socket = IO.socket(BASE_URL)
        socket?.connect()
        socket?.on("event", socketEventListener)
    }
}

abstract class ResponseEvent(@SerializedName("id") val eventId: String? = null)

class GameJoinEvent(@SerializedName("room") val room: GameRoom) : ResponseEvent()

class MemberUpdateEvent(
    @SerializedName("message") val message: String,
    @SerializedName("room") val room: GameRoom
) : ResponseEvent()

class TurnUpdateEvent(
    @SerializedName("user") val user: String,
    @SerializedName("number") val number: Int,
    @SerializedName("hasWon") val hasWon: Boolean = false,
    @SerializedName("turn") val turn: Int
): ResponseEvent()

class GameStateChangeEvent(@SerializedName("state") val state: Int) : ResponseEvent()

class NumberStackChangeEvent : ResponseEvent(LocalEvents.NUMBER_STACK_CHANGE_EVENT)