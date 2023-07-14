package com.cafedroid.bingo_android

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

object LocalEvents {
    const val NUMBER_STACK_CHANGE_EVENT = "100"
    const val BINGO_BUTTON_CHANGE_EVENT = "101"
}

const val GAME_JOIN_EVENT = 1001
const val MEMBER_UPDATE_EVENT = 1003
const val GAME_STATE_CHANGE_EVENT = 1005
const val TURN_UPDATE_EVENT = 1006
const val GAME_WIN_EVENT = 1007
const val GAME_LOCK_EVENT = 1008
const val SOCKET_ERROR_EVENT = 1009

object ErrorCode {
    const val MEMBER_LIMIT_REACHED = 2001
    const val USERNAME_TAKEN = 2002
    const val GAME_EXPIRED = 2003
}

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

        GAME_LOCK_EVENT -> {
            val gameLockEvent = Gson().fromJson(it[0].toString(), GameLockEvent::class.java)
            EventBus.getDefault().post(gameLockEvent)
        }

        GAME_WIN_EVENT -> {
            val gameWinEvent = Gson().fromJson(it[0].toString(), GameWinEvent::class.java)
            resetRows()
            EventBus.getDefault().post(gameWinEvent)
        }

        SOCKET_ERROR_EVENT -> {
            val errorEvent = Gson().fromJson(it[0].toString(), SocketErrorEvent::class.java)
            EventBus.getDefault().post(errorEvent)
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
        socket?.on("hello", socketEventListener)
    }
}

const val DEFAULT_ERROR_MESSAGE = "Something went wrong. Try again later."

class SocketErrorEvent(
    @SerializedName("code") val code: Int = -1,
    @SerializedName("message") val message: String = DEFAULT_ERROR_MESSAGE
) : ResponseEvent()

abstract class ResponseEvent(@SerializedName("id") val eventId: String? = null)

class GameJoinEvent(@SerializedName("room") val room: GameRoom) : ResponseEvent()

class MemberUpdateEvent(
    @SerializedName("message") val message: String,
    @SerializedName("room") val room: GameRoom
) : ResponseEvent()

class TurnUpdateEvent(
    @SerializedName("user") val user: String,
    @SerializedName("number") val number: Int,
    @SerializedName("winner") val winner: String?,
    @SerializedName("turn") val turn: Int
) : ResponseEvent()

class GameStateChangeEvent(@SerializedName("state") val state: Int) : ResponseEvent()

class GameLockEvent : ResponseEvent()

class GameWinEvent(
    @SerializedName("user") val user: String,
    @SerializedName("timeTaken") val timeTaken: Long
) : ResponseEvent()

class NumberStackChangeEvent : ResponseEvent(LocalEvents.NUMBER_STACK_CHANGE_EVENT)

class ButtonGlowEvent(val enable: Boolean) : ResponseEvent(LocalEvents.BINGO_BUTTON_CHANGE_EVENT)