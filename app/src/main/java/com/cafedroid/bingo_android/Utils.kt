package com.cafedroid.bingo_android

import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://d81a1cb8.ngrok.io"

const val GAME_JOIN_EVENT = 1001
const val MEMBER_UPDATE_EVENT = 1003
const val NUMBER_SEND_EVENT = 1004

var USERNAME = ""

val NUMBER_STACK: Stack<Int> = Stack()

val MOD_ARRAY = mutableListOf(0, 0, 0, 0, 0)
val DIV_ARRAY = mutableListOf(0, 0, 0, 0, 0)

fun initNumberStack() {
    NUMBER_STACK.clear()
    for (i in 25 downTo 1) {
        NUMBER_STACK.push(i)
    }
}

fun popNumberStack(): Int? {
    if (NUMBER_STACK.isEmpty().not()) EventBus.getDefault().post(NumberStackChangeEvent())
    return if (NUMBER_STACK.isEmpty().not()) NUMBER_STACK.pop() else null
}

fun pushToNumberStack(num: Int) {
    NUMBER_STACK.push(num)
    EventBus.getDefault().post(NumberStackChangeEvent())
}

fun Any.getLoggerTag(): String {
    return this.javaClass.simpleName
}

fun registerNumberToGame(num: Int, pos: Int) {
    MOD_ARRAY[pos % 5]++
    DIV_ARRAY[pos / 5]++
    BingoSocket.socket?.let {
        it.emit("push", JSONObject().apply {
            put("user", USERNAME)
            put("id", ActiveGameRoom.activeRoom?.roomId)
            put("number", num)
            put("hasWon", checkWinner())
        })
    }
}

object ApiConstants {
    const val ID = "id"
    const val NAME = "name"
    const val USER = "user"
}

object ActiveGameRoom {
    var activeRoom: GameRoom? = null
}

fun isAdmin() = ActiveGameRoom.activeRoom?.roomAdmin == USERNAME

private val socketEventListener: Emitter.Listener = Emitter.Listener {
    Log.e("Utils", "${it[0]}")
    when (val eventId = (it[0] as JSONObject)["id"]) {
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
        NUMBER_SEND_EVENT -> {
            val bingoEvent = Gson().fromJson(it[0].toString(), BingoNumberUpdateEvent::class.java)
            EventBus.getDefault().post(bingoEvent)
        }
        else -> {
            Log.e("SocketEventListener", "$eventId not handled")
        }
    }
}

fun checkWinner(): Boolean = countRows() >= 5

fun countRows(): Int {
    var rows = 0
    MOD_ARRAY.filter {
        it == 5
    }.map {
        rows++
    }
    DIV_ARRAY.filter {
        it == 5
    }.map {
        rows++
    }
    return rows
}

object BingoSocket {
    var socket: Socket? = null

    fun connect() {
        socket = IO.socket(BASE_URL)
        socket?.connect()
        socket?.on("event", socketEventListener)
    }
}

data class GameRoom(
    @SerializedName("_id") val roomId: String,
    @SerializedName("name") val roomName: String,
    @SerializedName("admin") var roomAdmin: String,
    @SerializedName("users") var roomMembers: List<String>,
    @SerializedName("state") var roomState: Int,
    @SerializedName("turn") var userTurn: Int,
    @Transient var hasWon: Boolean = false
)

data class BingoNumber(
    var number: Int = 0,
    var xPosition: Int = 0,
    var yPosition: Int = 0,
    var isDone: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (other is BingoNumber)
            return this.number == other.number && (this.xPosition == other.xPosition || this.yPosition == other.yPosition)

        return false
    }

    override fun hashCode(): Int {
        var result = number
        result = 31 * result + xPosition
        result = 31 * result + yPosition
        return result
    }

}

enum class GameState(val value: Int) {
    INACTIVE(100),
    ACTIVE(200),
    READY(300),
    IN_GAME(400),
    EXPIRED(500);

    companion object {
        fun getGameStateByValue(value: Int?): GameState {
            return when (value) {
                100 -> INACTIVE
                200 -> ACTIVE
                300 -> READY
                400 -> IN_GAME
                else -> EXPIRED
            }
        }
    }
}