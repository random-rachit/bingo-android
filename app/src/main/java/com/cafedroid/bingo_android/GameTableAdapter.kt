package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cafedroid.bingo_android.databinding.GameItemBinding

class GameTableAdapter(private val mContext: Context) :
    RecyclerView.Adapter<GameTableAdapter.BingoViewHolder>() {

    var mList = MutableList(25) { BingoNumber() }

    var isTableLocked = false

    init {
        initNumberStack()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BingoViewHolder {
        return BingoViewHolder(
            GameItemBinding.inflate(LayoutInflater.from(mContext))
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: BingoViewHolder, position: Int) {
        holder.setContent(mList[position])
    }

    fun toggleTableLock(lock: Boolean) {
        isTableLocked = lock
    }

    inner class BingoViewHolder(private val binding: GameItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setContent(bingoNumber: BingoNumber) {
            binding.tvBingoNumber.text =
                if (bingoNumber.number == 0) "·" else bingoNumber.number.toString()
            if (bingoNumber.isDone) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        mContext,
                        R.color.colorPrimary
                    )
                )

                binding.tvBingoNumber.setTextColor(
                    ContextCompat.getColor(
                        mContext,
                        android.R.color.white
                    )
                )
            }

            binding.tvBingoNumber.setOnClickListener {
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
                            markDone(bingoNumber.number, true)
                            toggleTableLock(true)
                            itemView.setBackgroundColor(
                                ContextCompat.getColor(
                                    mContext,
                                    R.color.colorPrimary
                                )
                            )
                            binding.tvBingoNumber.setTextColor(
                                ContextCompat.getColor(
                                    mContext,
                                    android.R.color.white
                                )
                            )
                        }
                    }

                    else -> {
                    }
                }
            }
        }

    }

    fun markDone(num: Int, push: Boolean) {
        mList.first {
            it.number == num
        }.apply {
            if (!isDone) {
                isDone = true
                val pos = mList.indexOf(this)
                notifyItemChanged(pos)
                registerNumberToGame(pos)
                if (push) pushNumberToGame(num)
            }
        }
    }
}