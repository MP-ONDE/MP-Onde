// UserAdapter.kt
package com.seoultech.onde

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView // 필요 시 추가
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nicknameTextView: TextView = itemView.findViewById(R.id.nicknameTextView)
        val smallTalkTextView: TextView = itemView.findViewById(R.id.smallTalkTextView)
        val rssiTextView: TextView = itemView.findViewById(R.id.rssiTextView)
        val ootdTextView: TextView = itemView.findViewById(R.id.ootdTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val user = users[position]
                    // 여기서 직접 인텐트를 생성하여 ProfileActivity를 시작합니다.
                    val intent = Intent(itemView.context, ProfileActivity::class.java)
                    intent.putExtra("userIdHash", user.userIdHash)
                    itemView.context.startActivity(intent)
                } else {
                    Log.e("UserAdapter", "유효하지 않은 위치: $position")

                }
            }
        }

        fun bind(user: User) {
            nicknameTextView.text = user.nickname
            smallTalkTextView.text = user.smallTalk
            ootdTextView.text = user.ootd
            rssiTextView.text = "RSSI: ${user.rssi}"
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nicknameTextView.text = user.nickname
        holder.smallTalkTextView.text = user.smallTalk
        holder.rssiTextView.text = "RSSI: ${user.rssi}"
        holder.ootdTextView.text = user.ootd

    }

    override fun getItemCount(): Int {
        return users.size
    }
}
