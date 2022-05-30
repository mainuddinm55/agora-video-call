package info.learncoding.arogavideocall.channel

sealed class ChannelState {
    object Lobby : ChannelState()
    data class Connecting(val channel: String) : ChannelState()
    data class Connected(val channel: String) : ChannelState()
    object Disconnected : ChannelState()
    data class ConnectionFailed(val message: String?) : ChannelState()
}
