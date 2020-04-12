package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.member_item.view.*

class MemberListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>() {

    private val mList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.member_item, parent, false)
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.setContent()
    }

    fun setMemberList(list: List<String>) {
        this.mList.clear()
        mList.addAll(list)
        notifyDataSetChanged()
    }

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setContent() {
            Glide.with(mContext).load(ROBOHASH_URL + mList[adapterPosition])
                .into(itemView.iv_display_bot)
            itemView.tv_display_name.text = mList[adapterPosition]
        }
    }
}