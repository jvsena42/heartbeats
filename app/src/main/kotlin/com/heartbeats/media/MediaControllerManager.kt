package com.heartbeats.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls whatever media app is currently playing via [MediaSessionManager]. Works with
 * any media app (Spotify, YT Music, Pocket Casts, …) — nothing is hardcoded. Reading active
 * sessions requires the user to have granted Notification access to Heartbeats.
 *
 * To avoid touching media the user paused themselves, [pauseActiveMedia] records which
 * packages it paused and [resumePausedMedia] only resumes those.
 */
@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sessionManager: MediaSessionManager? = context.getSystemService()
    private val listenerComponent = ComponentName(context, HeartbeatsNotificationListenerService::class.java)
    private val pausedByUs = mutableSetOf<String>()

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS) ?: return false
        return enabled.split(SEPARATOR).any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == context.packageName
        }
    }

    /** Pauses all currently-playing media sessions. Returns true if anything was paused. */
    fun pauseActiveMedia(): Boolean {
        var pausedAny = false
        activeControllers().forEach { controller ->
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
                pausedByUs += controller.packageName
                pausedAny = true
            }
        }
        return pausedAny
    }

    /** Resumes only the sessions Heartbeats previously paused. */
    fun resumePausedMedia() {
        if (pausedByUs.isEmpty()) return
        val byPackage = activeControllers().associateBy { it.packageName }
        pausedByUs.forEach { pkg -> byPackage[pkg]?.transportControls?.play() }
        pausedByUs.clear()
    }

    /** Forgets any tracked paused sessions without touching playback (e.g. when the workout stops). */
    fun clearTracking() {
        pausedByUs.clear()
    }

    private fun activeControllers(): List<MediaController> = runCatching {
        sessionManager?.getActiveSessions(listenerComponent).orEmpty()
    }.getOrDefault(emptyList())

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        const val SEPARATOR = ":"
    }
}
