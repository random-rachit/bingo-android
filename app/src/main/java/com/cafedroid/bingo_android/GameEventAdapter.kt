package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.game_event_item.view.*

class GameEventAdapter(val context: Context) :
    RecyclerView.Adapter<GameEventAdapter.GameEventViewHolder>() {

    private val eventList = mutableListOf<GameEvent>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameEventViewHolder {
        return GameEventViewHolder(
            LayoutInflater.from(context).inflate(R.layout.game_event_item, parent, false)
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

    inner class GameEventViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun setContent(event: GameEvent) {
            Glide.with(context).load(ROBO_HASH_URL + event.user)
                .into(itemView.event_icon)
            itemView.tv_event.text = event.event
        }
    }
}