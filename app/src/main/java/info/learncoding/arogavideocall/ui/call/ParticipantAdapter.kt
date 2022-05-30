package info.learncoding.arogavideocall.ui.call

import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.learncoding.arogavideocall.databinding.ThumbParticipantViewBinding
import info.learncoding.arogavideocall.partifipant.UserStatusData

class ParticipantAdapter : ListAdapter<UserStatusData, ParticipantAdapter.ParticipantViewHolder>(
    diffUtilCallback
) {
    companion object {
        val diffUtilCallback = object : DiffUtil.ItemCallback<UserStatusData>() {
            override fun areItemsTheSame(
                oldItem: UserStatusData,
                newItem: UserStatusData
            ): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(
                oldItem: UserStatusData,
                newItem: UserStatusData
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ThumbParticipantViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }


    inner class ParticipantViewHolder(private val binding: ThumbParticipantViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(userStatusData: UserStatusData) {
            binding.videoView.children.forEach {
                if (it is SurfaceView) {
                    if (it == userStatusData.view && !userStatusData.isVideoMuted) return
                    binding.videoView.removeView(it)
                }
            }
            if (userStatusData.videoInfo != null) {
                val target = userStatusData.view
                if (target != null && !userStatusData.isVideoMuted) {
                    stripView(target)
                    target.setZOrderOnTop(true)
                    binding.videoView.addView(target)
                    binding.thumbImageView.isVisible = false
                } else {
                    target?.setZOrderOnTop(false)
                    binding.videoView.removeAllViews()
                    binding.thumbImageView.isVisible = true
                }
            } else {
                binding.videoView.removeAllViews()
                binding.thumbImageView.isVisible = true
            }
            binding.muteImageView.isVisible = userStatusData.isMuted
        }
    }

    fun stripView(view: SurfaceView) {
        val parent = view.parent
        if (parent != null) {
            (parent as FrameLayout).removeView(view)
        }
    }
}