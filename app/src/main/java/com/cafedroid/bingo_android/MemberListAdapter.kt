package com.cafedroid.bingo_android

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cafedroid.bingo_android.databinding.MemberItemBinding

class MemberListAdapter(private val mContext: Context) :
    RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>() {

    private val mList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(
            MemberItemBinding.inflate(LayoutInflater.from(mContext), parent, false)
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.setContent(mList[position])
    }

    fun setMemberList(list: List<String>) {
        this.mList.clear()
        mList.addAll(list)
        notifyDataSetChanged()
    }

    class MemberViewHolder(private val binding: MemberItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setContent(data: String) {
            Glide.with(binding.root.context).load(ROBO_HASH_URL + data)
                .into(binding.ivDisplayBot)
            binding.tvDisplayName.text = data
        }
    }
}