package com.cafedroid.bingo_android

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.game_item.view.*

class GameTableAdapter(private val mContext: Context) :
    RecyclerView.Adapter<GameTableAdapter.BingoViewHolder>() {

    var mList = MutableList(25) { BingoNumber() }

    var isTableLocked = false

    init {
        initNumberStack()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingoViewHolder {
        return BingoViewHolder(
            LayoutInflater.from(mContext)
                .inflate(R.layout.game_item, parent, false)
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: BingoViewHolder, position: Int) {
        holder.setContent()
    }

    fun toggleTableLock(lock: Boolean) {
        isTableLocked = lock
    }

    inner class BingoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setContent() {
            val bingoNumber = mList[adapterPosition]
            itemView.tv_bingo_number.text =
                if (mList[adapterPosition].number == 0) "·" else mList[adapterPosition].number.toString()
            if (bingoNumber.isDone)
                itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        mContext,
                        android.R.color.holo_red_dark
                    )
                )

            itemView.tv_bingo_number.setOnClickListener {
                if (isTableLocked || bingoNumber.isDone) return@setOnClickListener
                val num = bingoNumber.number
                when (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState)) {
                    GameState.READY -> {
                        if (num == 0) {
                            val stackNumber = (popNumberStack() ?: 0)
                            mList[adapterPosition] = BingoNumber(stackNumber)
                            notifyItemChanged(adapterPosition)
                        } else {
                            pushToNumberStack(num)
                            mList[adapterPosition] = BingoNumber()
                            notifyItemChanged(adapterPosition)
                        }
                    }
                    GameState.IN_GAME -> {
                        if (ActiveGameRoom.activeRoom?.roomMembers?.get(
                                ActiveGameRoom.activeRoom?.userTurn ?: 0
                            ) == USERNAME
                        ) {
                            markDone(mList[adapterPosition].number, true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                itemView.setBackgroundColor(
                                    ContextCompat.getColor(
                                        mContext,
                                        android.R.color.holo_red_dark
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        }

    }

    fun markDone(num: Int, send: Boolean) {
        mList.first {
            it.number == num
        }.apply {
            if (!isDone) {
                isDone = true
                val pos = mList.indexOf(this)
                notifyItemChanged(pos)
                registerNumberToGame(num, pos, send)
            }
        }
    }
}