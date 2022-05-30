package info.learncoding.arogavideocall.channel

import info.learncoding.arogavideocall.partifipant.UserStatusData

data class ChannelViewState(
    val primaryParticipant: UserStatusData,
    val remoteParticipants: List<UserStatusData>? = null,
    val isAudioMute:Boolean = false,
    val isVideoMute:Boolean = false
)