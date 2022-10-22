package com.example.pcap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.pcap.fragments.ProfileFragment

class PostAdapter(val context: Context, val posts: MutableList<Post>)
    : RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_posts, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostAdapter.ViewHolder, position: Int) {
        val post = posts.get(position)
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView
        val ivProfilePic: ImageView
        val ivImage: ImageView
        val tvDescription: TextView
        val tvTimestamp: TextView

        init {
            tvUsername = itemView.findViewById(R.id.tvUsername)
            ivProfilePic = itemView.findViewById(R.id.ivProfilePic)
            ivImage = itemView.findViewById(R.id.ivImage)
            tvDescription = itemView.findViewById(R.id.tvDescription)
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp)
        }

        fun bind(post: Post) {
            tvDescription.text = post.getDescription()
            tvUsername.text = post.getUser()?.username
            tvTimestamp.text = TimeFormatter.getTimeDifference(post.createdAt.toString())
            if (post.getUser()?.getParseFile(ProfileFragment.KEY_PFP) != null) {
                Glide.with(itemView.context)
                    .load(post.getUser()?.getParseFile(ProfileFragment.KEY_PFP)?.url)
                    .transform(CircleCrop())
                    .into(ivProfilePic)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.instagram_user_filled_24)
                    .transform(CircleCrop())
                    .into(ivProfilePic)
            }

            Glide.with(itemView.context).load(post.getImage()?.url).into(ivImage)
        }
    }
    fun clear() {
        posts.clear()
        notifyDataSetChanged()
    }

    // Add a list of items -- change to type used
    fun addAll(tweetList: List<Post>) {
        posts.addAll(tweetList)
        notifyDataSetChanged()
    }
}