package com.cafedroid.bingo_android

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*

var BASE_URL = "https://f2a2c6c8.ngrok.io"
var WEB_URL = "https://bingo.cafedroid.biz"
const val ROBO_HASH_URL = "https://robohash.org/"

var USERNAME = ""


val NUMBER_STACK: Stack<Int> = Stack()

var MOD_ARRAY = MutableList(5) { 0 }
var DIV_ARRAY = MutableList(5) { 0 }
var DIAGONAL_1 = 0
var DIAGONAL_2 = 0

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

fun pushNumberToGame(num: Int) {
    BingoSocket.socket?.let {
        it.emit(SocketAction.ACTION_PUSH, JSONObject().apply {
            put(ApiConstants.USER, USERNAME)
            put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
            put(ApiConstants.NUMBER, num)
        })
    }
}

fun registerNumberToGame(pos: Int) {
    val mod = pos % 5
    val div = pos / 5
    if (mod == div) DIAGONAL_1++
    if (mod + div == 4) DIAGONAL_2++
    MOD_ARRAY[mod]++
    DIV_ARRAY[div]++
    if (checkWinner()) EventBus.getDefault().post(ButtonGlowEvent(true))
}

object SocketAction {

    const val ACTION_WIN = "win"
    const val ACTION_ADMIN = "adminAction"
    const val ACTION_START = "start"
    const val ACTION_LEAVE_ROOM = "leave"
    const val ACTION_CREATE = "create"
    const val ACTION_JOIN = "join"
    const val ACTION_PUSH = "push"

}

object ApiConstants {
    const val TIME_TAKEN = "time"
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

fun resetRows() {
    MOD_ARRAY = MutableList(5) { 0 }
    DIV_ARRAY = MutableList(5) { 0 }
    DIAGONAL_1 = 0
    DIAGONAL_2 = 0
}

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
    if (DIAGONAL_1 == 5) rows++
    if (DIAGONAL_2 == 5) rows++

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

fun isMyTurn(): Boolean = getActiveUser() == USERNAME

fun getActiveUser(): String {
    ActiveGameRoom.activeRoom?.apply {
        return roomMembers[userTurn]
    }
    return ""
}

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

fun TextView.setCustomFont(context: Context) {
    val am = context.assets
    val typeface = Typeface.createFromAsset(am, "fonts/pacifico.ttf")
    setTypeface(typeface)
}

class GameEvent(val user: String, val event: String)