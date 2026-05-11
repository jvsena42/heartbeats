package com.heartbeats.media

import android.service.notification.NotificationListenerService

/**
 * Declared only so the user can grant "Notification access", which is what unlocks
 * [android.media.session.MediaSessionManager.getActiveSessions] on Wear OS. Heartbeats
 * never reads notification content — this service has no behavior of its own.
 */
class HeartbeatsNotificationListenerService : NotificationListenerService()
