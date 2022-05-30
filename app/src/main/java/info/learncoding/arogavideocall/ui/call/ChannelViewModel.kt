package info.learncoding.arogavideocall.ui.call

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import info.learncoding.arogavideocall.channel.ChannelManager
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelManager: ChannelManager
) : ViewModel() {

    val messages = channelManager.messages
    val channelViewState = channelManager.channelViewState
    val channelState = channelManager.channelState

    fun joinChannel() {
        channelManager.joinChannel("chats", 0)
    }

    override fun onCleared() {
        super.onCleared()
        channelManager.leaveChannel()
    }

    fun toggleAudio() {
        channelManager.toggleAudio()
    }

    fun toggleVideo() {
        channelManager.toggleVideo()
    }

    fun switchCamera() {
        channelManager.switchCamera()
    }

    fun leaveChannel() {
        channelManager.leaveChannel()
    }

    fun sendMessage(message: String) {
        channelManager.sendMessage(message)
    }
}