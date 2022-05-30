package info.learncoding.arogavideocall.ui.call

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import info.learncoding.arogavideocall.R
import info.learncoding.arogavideocall.channel.ChannelState
import info.learncoding.arogavideocall.channel.ChannelViewState
import info.learncoding.arogavideocall.databinding.ActivityCallBinding
import info.learncoding.arogavideocall.partifipant.UserStatusData
import info.learncoding.arogavideocall.uitls.toast


@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        fun buildIntent(context: Context): Intent {
            return Intent(context, CallActivity::class.java)
        }
    }

    private val viewModel by viewModels<ChannelViewModel>()
    private lateinit var binding: ActivityCallBinding
    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_call)

        setKeepScreenFlag()

        initClickEvent()
        initRecyclerView()
        observeChannelState()
        observeChannelViewState()
        observeStreamMessages()

        viewModel.joinChannel()

        observeMessageEditText()
    }

    private fun initClickEvent() {
        binding.audioControlImageView.setOnClickListener {
            viewModel.toggleAudio()
        }
        binding.videoControlImageView.setOnClickListener {
            viewModel.toggleVideo()
        }
        binding.cameraSwitchImageView.setOnClickListener {
            viewModel.switchCamera()
        }
        binding.callEndImageView.setOnClickListener {
            viewModel.leaveChannel()
        }
        binding.sendImageView.setOnClickListener {
            viewModel.sendMessage(binding.msgEditText.text.toString())
            binding.msgEditText.text = null
        }
    }

    private fun setKeepScreenFlag() {
        // So calls can be answered when screen is locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    private fun observeMessageEditText() {
        binding.msgEditText.addTextChangedListener {
            binding.sendImageView.isVisible = it.toString().isNotEmpty()
        }
    }

    private fun initRecyclerView() {
        participantAdapter = ParticipantAdapter()
        binding.remoteVideoThumbnails.apply {
            adapter = participantAdapter
        }
        messageAdapter = MessageAdapter();
        binding.messageRecyclerView.apply {
            adapter = messageAdapter
        }
    }

    private fun observeStreamMessages() {
        viewModel.messages.observe(this) {
            messageAdapter.submitList(it)
        }
    }

    private fun observeChannelState() {
        viewModel.channelState.observe(this) {
            Log.d(TAG, "observeChannelState: $it")
            when (it) {
                is ChannelState.Connected -> toast("Connected")
                is ChannelState.Connecting -> toast("Connecting")
                is ChannelState.ConnectionFailed -> toast("Connection failed")
                ChannelState.Lobby -> Unit
                ChannelState.Disconnected -> finish()
            }
        }
    }

    private fun observeChannelViewState() {
        viewModel.channelViewState.observe(this) {
            Log.d(TAG, "observeChannelViewState: $it")
            renderPrimaryView(it.primaryParticipant)
            renderThumbView(it.remoteParticipants)
            updateUiControl(it)
        }
    }

    private fun renderPrimaryView(primaryParticipant: UserStatusData) {
        binding.primaryView.children.forEach {
            if (it is SurfaceView) {
                binding.primaryView.removeView(it)
            }
        }
        if (primaryParticipant.videoInfo != null) {
            Log.d(TAG, "renderPrimaryView: has video info")
            val target = primaryParticipant.view
            if (target != null) {
                stripView(target)
                binding.primaryView.addView(target)
                Log.d(TAG, "renderPrimaryView: render video")
            }
        } else {
            Log.d(TAG, "renderPrimaryView: no video info")
        }
    }

    private fun stripView(view: SurfaceView) {
        val parent = view.parent
        if (parent != null) {
            (parent as FrameLayout).removeView(view)
        }
    }

    private fun renderThumbView(remoteParticipants: List<UserStatusData>?) {
        remoteParticipants ?: return
        participantAdapter.submitList(remoteParticipants)
    }

    private fun updateUiControl(channelViewState: ChannelViewState) {
        val micResId = if (channelViewState.isAudioMute) R.drawable.ic_baseline_mic_off_24
        else R.drawable.ic_baseline_mic_24
        val videoResId = if (channelViewState.isVideoMute) R.drawable.ic_baseline_videocam_off_24
        else R.drawable.ic_baseline_videocam_24
        Log.d(
            TAG,
            "updateUiControl: audio: ${channelViewState.isAudioMute} video: ${channelViewState.isVideoMute}"
        )
        binding.videoControlImageView.setImageResource(videoResId)
        binding.audioControlImageView.setImageResource(micResId)
    }

}