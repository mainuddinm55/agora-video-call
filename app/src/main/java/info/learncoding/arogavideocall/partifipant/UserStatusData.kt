package info.learncoding.arogavideocall.partifipant

import android.view.SurfaceView

data class UserStatusData(
    val uid: Int,
    val view: SurfaceView? = null,
    val videoInfo: VideoInfoData? = null,
    val isLocal: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isVideoMuted: Boolean = false
)