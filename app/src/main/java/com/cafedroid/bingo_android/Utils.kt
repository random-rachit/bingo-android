package com.cafedroid.bingo_android

import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*

const val BASE_URL = "https://174efd50.ngrok.io"
const val ROBOHASH_URL = "https://robohash.org/"

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

fun registerNumberToGame(num: Int, pos: Int, sendToSocket: Boolean = false) {
    MOD_ARRAY[pos % 5]++
    DIV_ARRAY[pos / 5]++
    val hasWon = countRows() >= 5
    if (sendToSocket) {
        BingoSocket.socket?.let {
            it.emit(SocketAction.ACTION_PUSH, JSONObject().apply {
                put(ApiConstants.USER, USERNAME)
                put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                put(ApiConstants.NUMBER, num)
                put(ApiConstants.HAS_WON, hasWon)
            })
        }
    }
}

object SocketAction {

    const val ACTION_ADMIN = "adminAction"
    const val ACTION_START = "start"
    const val ACTION_LEAVE_ROOM = "leave"
    const val ACTION_CREATE = "create"
    const val ACTION_JOIN = "join"
    const val ACTION_PUSH = "push"

}

object ApiConstants {
    const val HAS_WON = "hasWon"
    const val NUMBER = "number"
    const val ID = "id"
    const val NAME = "name"
    const val USER = "user"
}

object ActiveGameRoom {
    var activeRoom: GameRoom? = null
}

fun isAdmin() = ActiveGameRoom.activeRoom?.roomAdmin == USERNAME

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

class GameRoom(
    @SerializedName("_id") val roomId: String,
    @SerializedName("name") val roomName: String,
    @SerializedName("admin") var roomAdmin: String,
    @SerializedName("users") var roomMembers: List<String>,
    @SerializedName("state") var roomState: Int,
    @SerializedName("turn") var userTurn: Int
)

data class BingoNumber(var number: Int = 0, var isDone: Boolean = false)

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

enum class AdminAction(val value: Int) {
    LAUNCH_GAME(900), INVALID(-1);

    companion object {
        fun getGameStateByValue(value: Int?): AdminAction {
            return when (value) {
                100 -> LAUNCH_GAME
                else -> INVALID
            }
        }
    }
}