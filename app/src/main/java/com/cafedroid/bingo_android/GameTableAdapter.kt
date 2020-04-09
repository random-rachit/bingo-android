package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.game_item.view.*

class GameTableAdapter(private val mContext: Context) :
    RecyclerView.Adapter<GameTableAdapter.BingoViewHolder>() {

    var mList =
        mutableListOf(
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber(),
            BingoNumber()
        )

    private var isLocked = false

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

    fun lockAdapter(lock: Boolean) {
        isLocked = true
    }

    inner class BingoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setContent() {
            itemView.tv_bingo_number.text =
                if (mList[adapterPosition].number == 0) "·" else mList[adapterPosition].number.toString()

            itemView.tv_bingo_number.setOnClickListener {
                if (isLocked) return@setOnClickListener
                val number = mList[adapterPosition].number
                when (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState)) {
                    GameState.READY -> {
                        if (number == 0) {
                            val num = (popNumberStack() ?: 0)
                            itemView.tv_bingo_number.text = num.toString()
                            mList[adapterPosition] =
                                BingoNumber(num, adapterPosition % 5, adapterPosition / 5)
                        } else {
                            pushToNumberStack(number)
                            mList[adapterPosition] = BingoNumber()
                            itemView.tv_bingo_number.text =
                                if (mList[adapterPosition].number == 0) "·" else mList[adapterPosition].number.toString()
                        }
                    }
                    GameState.IN_GAME -> {
                        registerNumberToGame(adapterPosition)
                        itemView.setBackgroundColor(
                            ContextCompat.getColor(
                                mContext,
                                android.R.color.holo_red_dark
                            )
                        )
                    }
                    else -> {
                    }
                }
            }
        }

    }
}