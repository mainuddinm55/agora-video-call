package info.learncoding.arogavideocall.partifipant

import io.agora.rtc.IRtcEngineEventHandler

class ParticipantManager {

    private val users = mutableListOf<UserStatusData>()

    val thumbUsers: List<UserStatusData>
        get() {
            return if (users.size > 1) {
                users.filter { it.isLocal && !it.isPinned }
            } else {
                emptyList()
            }
        }

    var primaryUser: UserStatusData
        private set

    init {
        val localUser = UserStatusData(
            uid = 1, isLocal = true, videoInfo = VideoInfoData(
                320, 240, 0, 15, 0, 0
            )
        )
        users.add(localUser)
        primaryUser = localUser
    }

    fun addUser(userStatusData: UserStatusData) {
        if (users.find { it.uid == userStatusData.uid } != null) return
        this.users.add(userStatusData)
        updatePrimaryUser()
    }

    fun removeUser(id: Int) {
        users.removeAll { it.uid == id }
        updatePrimaryUser()
    }

    fun muteUser(id: Int, isMuted: Boolean) {
        getUser(id)?.copy(isMuted = isMuted)?.let {
            updateUser(it)
        }
    }

    fun videoOff(uid: Int, isVideoOff: Boolean) {
        getUser(uid)?.copy(isVideoMuted = isVideoOff)?.let {
            updateUser(it)
        }
    }

    fun pinUser(uid: Int, isPinned: Boolean) {
        getUser(uid)?.copy(isPinned = isPinned)?.let {
            updateUser(it)
        }
    }

    fun clearRemoteUser() {
        users.removeAll { !it.isLocal }
    }

    fun updateLocalUserId(id: Int) {
        users.find { it.isLocal }?.copy(uid = id)?.let { user ->
            updateUser(user) { it.isLocal }
        }
    }

    fun updateLocalUser(user: UserStatusData) {
        users.find { it.isLocal }?.let {
            updateUser(user.copy(isLocal = true))
        }
    }

    fun getLocalUser(): UserStatusData? {
        return users.find { it.isLocal }
    }

    private fun getUser(id: Int): UserStatusData? = users.find { it.uid == id }

    private fun updateUser(
        user: UserStatusData,
        matchPredicate: (UserStatusData) -> Boolean = { it.uid == user.uid }
    ) {
        users.indexOfFirst(matchPredicate).let { index ->
            if (index > -1) {
                users[index] = user
                updatePrimaryUser()
            }
        }
    }

    private fun updatePrimaryUser() {
        primaryUser = users.find { it.isPinned } ?: users.find { !it.isLocal }
                ?: users.find { it.isLocal }
                ?: users[0]
    }

    fun addRemoteVideoInfo(stats: IRtcEngineEventHandler.RemoteVideoStats?): Boolean {
        stats ?: return false
        getUser(stats.uid)?.let {
            if (it.videoInfo == null) {
                updateUser(
                    it.copy(
                        videoInfo = VideoInfoData(
                            stats.width,
                            stats.height,
                            stats.delay,
                            stats.rendererOutputFrameRate,
                            stats.receivedBitrate,
                            0
                        )
                    )
                )
                return true
            } else {
                return false
            }
        } ?: run { return false }
    }
}