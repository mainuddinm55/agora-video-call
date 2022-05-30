package info.learncoding.arogavideocall.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.learncoding.arogavideocall.channel.ChannelManager
import info.learncoding.arogavideocall.partifipant.ParticipantManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgoraModule {

    @Provides
    @Singleton
    fun provideParticipantManager(): ParticipantManager {
        return ParticipantManager()
    }

    @Provides
    @Singleton
    fun provideChannelManager(
        @ApplicationContext context: Context,
        participantManager: ParticipantManager
    ): ChannelManager {
        return ChannelManager(context, participantManager)
    }
}