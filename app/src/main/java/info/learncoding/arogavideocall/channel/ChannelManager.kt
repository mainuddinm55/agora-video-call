package info.learncoding.arogavideocall.channel

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import info.learncoding.arogavideocall.BuildConfig
import info.learncoding.arogavideocall.data.model.Message
import info.learncoding.arogavideocall.partifipant.ParticipantManager
import info.learncoding.arogavideocall.partifipant.UserStatusData
import info.learncoding.arogavideocall.uitls.AppConstraint
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.internal.EncryptionConfig
import io.agora.rtc.models.DataStreamConfig
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE
import io.agora.rtc.video.VideoEncoderConfiguration.VideoDimensions


open class ChannelManager(
    private val context: Context,
    private val participantManager: ParticipantManager,
    private var initialState: ChannelViewState = ChannelViewState(participantManager.primaryUser),
    private var mutableMessages: MutableList<Message> = mutableListOf()
) {

    companion object {
        private const val TAG = "ChannelManager"
        private const val STREAM_ID = 887
    }

    private var channel: String? = null
    private var streamId: Int = 0
    private val rtcEventListener = RtcEventListener()

    private val rtcEngine = RtcEngine.create(context, BuildConfig.AGORA_APP_ID, rtcEventListener)

    private val _channelViewState = MutableLiveData(initialState)
    val channelViewState: LiveData<ChannelViewState> = _channelViewState

    private val _callState = MutableLiveData<ChannelState>(ChannelState.Lobby)
    val channelState: LiveData<ChannelState> = _callState

    private val _messages = MutableLiveData(mutableMessages.toList())
    val messages: LiveData<List<Message>> = _messages


    init {
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine.enableVideo()
        rtcEngine.enableAudioVolumeIndication(200, 3, false)
        doConfigEngine()
        val surfaceV = RtcEngine.CreateRendererView(context)
        preview(true, surfaceV, participantManager.primaryUser.uid)
        surfaceV.setZOrderOnTop(false)
        surfaceV.setZOrderMediaOverlay(false)
        participantManager.getLocalUser()?.let {
            participantManager.updateLocalUser(it.copy(view = surfaceV))
        }
        updateParticipantState()
    }

    private fun preview(start: Boolean, surfaceV: SurfaceView?, uid: Int) {
        if (start) {
            rtcEngine.setupLocalVideo(VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            rtcEngine.startPreview()
        } else {
            rtcEngine.stopPreview()
        }
    }

    private fun doConfigEngine() {
        val videoDimension: VideoDimensions = AppConstraint.VIDEO_DIMENSIONS[
                AppConstraint.DEFAULT_VIDEO_ENC_RESOLUTION_IDX]
        val videoFps: FRAME_RATE = AppConstraint.VIDEO_FPS[AppConstraint.DEFAULT_VIDEO_ENC_FPS_IDX]
        configEngine(videoDimension, videoFps, "", "AES-128-XTS")
    }

    private fun configEngine(
        videoDimension: VideoDimensions,
        fps: FRAME_RATE,
        encryptionKey: String?,
        encryptionMode: String
    ) {
        val config = EncryptionConfig()
        if (!TextUtils.isEmpty(encryptionKey)) {
            config.encryptionKey = encryptionKey
            if (TextUtils.equals(encryptionMode, "AES-128-XTS")) {
                config.encryptionMode = EncryptionConfig.EncryptionMode.AES_128_XTS
            } else if (TextUtils.equals(encryptionMode, "AES-256-XTS")) {
                config.encryptionMode = EncryptionConfig.EncryptionMode.AES_256_XTS
            }
            rtcEngine.enableEncryption(true, config)
        } else {
            rtcEngine.enableEncryption(false, config)
        }
        // Set the Resolution, FPS. Bitrate and Orientation of the video
        rtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                videoDimension,
                fps,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
            )
        )
    }

    fun joinChannel(channel: String, uid: Int) {
        this.channel = channel
        updateCallState(ChannelState.Connecting(this.channel!!))
        val accessToken = BuildConfig.AGORA_ACCESS_TOKEN
        rtcEngine.joinChannel(accessToken, channel, "OpenVCall", uid)
        streamId = rtcEngine.createDataStream(DataStreamConfig())
    }

    fun leaveChannel() {
        rtcEngine.leaveChannel()
        preview(false, null, 0)
    }

    fun sendMessage(message: String) {
        rtcEngine.sendStreamMessage(streamId, message.toByteArray())
        updateMessage("You", message)
    }

    private fun updateParticipantState() {
        updateChannelViewState(
            initialState.copy(
                primaryParticipant = participantManager.primaryUser,
                remoteParticipants = participantManager.thumbUsers,
                isAudioMute = participantManager.getLocalUser()?.isMuted ?: false,
                isVideoMute = participantManager.getLocalUser()?.isVideoMuted ?: false
            )
        )
    }

    private fun updateChannelViewState(viewState: ChannelViewState) {
        this.initialState = viewState
        _channelViewState.postValue(viewState)
    }

    private fun updateCallState(channelState: ChannelState) {
        _callState.postValue(channelState)
    }

    fun switchCamera() {
        rtcEngine.switchCamera()
    }

    fun toggleVideo() {
        val local = participantManager.getLocalUser() ?: return
        if (local.isVideoMuted) {
            rtcEngine.enableLocalVideo(true)
        } else {
            rtcEngine.enableLocalVideo(false)
        }
        participantManager.updateLocalUser(local.copy(isVideoMuted = !local.isVideoMuted))
        updateParticipantState()
    }

    fun toggleAudio() {
        val local = participantManager.getLocalUser() ?: return
        if (local.isMuted) {
            rtcEngine.enableLocalAudio(true)
        } else {
            rtcEngine.enableLocalAudio(false)
        }
        participantManager.updateLocalUser(local.copy(isMuted = !local.isMuted))
        updateParticipantState()
    }

    private fun updateMessage(uid: String?, message: String) {
        mutableMessages.add(Message(uid, message))
        _messages.postValue(mutableMessages.toList())
    }

    inner class RtcEventListener : IRtcEngineEventHandler() {

        override fun onUserEnableVideo(uid: Int, enabled: Boolean) {
            super.onUserEnableVideo(uid, enabled)
            participantManager.videoOff(uid, !enabled)
            updateParticipantState()
            Log.d(TAG, "onUserEnableVideo: $uid enable : $enabled")
        }

        override fun onUserMuteAudio(uid: Int, muted: Boolean) {
            super.onUserMuteAudio(uid, muted)
            participantManager.muteUser(uid, muted)
            updateParticipantState()
            Log.d(TAG, "onUserMuteAudio: $uid muted: $muted")
        }

        override fun onRemoteVideoStats(stats: RemoteVideoStats?) {
            super.onRemoteVideoStats(stats)
            val updated = participantManager.addRemoteVideoInfo(stats)
            if (updated) {
                updateParticipantState()
                Log.d(TAG, "onRemoteVideoStats: $stats")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.d(TAG, "onUserJoined: $uid")

            val surfaceV = RtcEngine.CreateRendererView(context)

            surfaceV.setZOrderOnTop(false)
            surfaceV.setZOrderMediaOverlay(false)
            rtcEngine.setupRemoteVideo(
                VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid)
            )

            participantManager.addUser(UserStatusData(uid, surfaceV))
            updateParticipantState()
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            participantManager.removeUser(uid)
            updateParticipantState()
            Log.d(TAG, "onUserOffline: $uid")
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
            participantManager.clearRemoteUser()
            updateParticipantState()
            updateCallState(ChannelState.Disconnected)
            Log.d(TAG, "onLeaveChannel: ")
        }

        override fun onError(err: Int) {
            super.onError(err)
            Log.d(TAG, "onError: $err")
            updateCallState(ChannelState.ConnectionFailed("Error: $err"))
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Log.d(TAG, "onConnectionLost: ")
            updateCallState(ChannelState.Disconnected)
        }

        override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
            super.onStreamMessage(uid, streamId, data)
            data ?: return
            Log.d(TAG, "onStreamMessage: ${String(data)}")
            updateMessage(uid = uid.toString(), String(data))
        }

        override fun onStreamMessageError(
            uid: Int,
            streamId: Int,
            error: Int,
            missed: Int,
            cached: Int
        ) {
            super.onStreamMessageError(uid, streamId, error, missed, cached)
            Log.d(TAG, "onStreamMessageError: $streamId $error")
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.d(TAG, "onJoinChannelSuccess: $uid channel $channel")
            participantManager.updateLocalUserId(uid)
            updateParticipantState()
            updateCallState(ChannelState.Connected(channel!!))
        }

    }

}