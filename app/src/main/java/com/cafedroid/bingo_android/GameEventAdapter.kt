package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cafedroid.bingo_android.databinding.GameEventItemBinding

class GameEventAdapter(val context: Context) :
    RecyclerView.Adapter<GameEventAdapter.GameEventViewHolder>() {

    private val eventList = mutableListOf<GameEvent>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameEventViewHolder {
        return GameEventViewHolder(
            GameEventItemBinding.inflate(LayoutInflater.from(context))
        )
    }

    override fun getItemCount(): Int {
        return eventList.size
    }

    override fun onBindViewHolder(holder: GameEventViewHolder, position: Int) {
        holder.setContent(eventList[position])
    }

    fun pushEvent(event: GameEvent) {
        eventList.add(event)
        notifyDataSetChanged()
    }

    inner class GameEventViewHolder(private val binding: GameEventItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setContent(event: GameEvent) {
            Glide.with(context).load(ROBO_HASH_URL + event.user)
                .into(binding.eventIcon)
            binding.tvEvent.text = event.event
        }
    }
}