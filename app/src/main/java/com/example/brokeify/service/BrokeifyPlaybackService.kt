package com.example.brokeify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.brokeify.MainActivity
import com.example.brokeify.model.Track

class BrokeifyPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "brokeify_playback_channel"
        const val NOTIFICATION_ID = 3487

        const val ACTION_SYNC = "com.example.brokeify.ACTION_SYNC"
        const val ACTION_PLAY_PAUSE = "com.example.brokeify.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.brokeify.ACTION_NEXT"
        const val ACTION_PREV = "com.example.brokeify.ACTION_PREV"
        const val ACTION_TOGGLE_LIKE = "com.example.brokeify.ACTION_TOGGLE_LIKE"
        const val ACTION_STOP = "com.example.brokeify.ACTION_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_ALBUM = "extra_album"
        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_ACCENT = "extra_accent"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_IS_LIKED = "extra_is_liked"

        var onPlayPause: (() -> Unit)? = null
        var onNext: (() -> Unit)? = null
        var onPrev: (() -> Unit)? = null
        var onToggleLike: (() -> Unit)? = null
        var playerProvider: (() -> ExoPlayer?)? = null
        var isLikedProvider: (() -> Boolean)? = null

        fun startOrUpdate(context: Context, track: Track?, isPlaying: Boolean, isLiked: Boolean = false) {
            val intent = Intent(context, BrokeifyPlaybackService::class.java).apply {
                action = ACTION_SYNC
                if (track != null) {
                    putExtra(EXTRA_TITLE, track.title)
                    putExtra(EXTRA_ARTIST, track.artist)
                    putExtra(EXTRA_ALBUM, track.album)
                    putExtra(EXTRA_FORMAT, track.format)
                    putExtra(EXTRA_ACCENT, track.accentColor)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                    putExtra(EXTRA_IS_LIKED, isLiked || track.isLiked)
                }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BrokeifyPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var mediaSession: MediaSession? = null
    private lateinit var notificationManager: NotificationManager

    // Cached state
    private var currentTitle: String = ""
    private var currentArtist: String = ""
    private var currentAlbum: String = ""
    private var currentFormat: String = "STEREO"
    private var currentAccent: Long = 0xFF00FF66L
    private var isPlayingState: Boolean = false
    private var isLikedState: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        initLocks()
        initMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Brokeify Background Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback controls and persistent audio streaming for Brokeify Docker"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Brokeify:MediaPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "Brokeify:MediaPlaybackWifiLock"
            )?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Exception) {
        }
    }

    private fun initMediaSession() {
        try {
            val player = playerProvider?.invoke()
            if (player != null && mediaSession == null) {
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    openIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                mediaSession = MediaSession.Builder(this, player)
                    .setSessionActivity(pendingIntent)
                    .build()
            }
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                onPlayPause?.invoke()
                isPlayingState = !isPlayingState
                updateForeground(isPlayingState)
                return START_STICKY
            }
            ACTION_NEXT -> {
                onNext?.invoke()
                return START_STICKY
            }
            ACTION_PREV -> {
                onPrev?.invoke()
                return START_STICKY
            }
            ACTION_TOGGLE_LIKE -> {
                onToggleLike?.invoke()
                isLikedState = !isLikedState
                updateForeground(isPlayingState)
                return START_STICKY
            }
            ACTION_STOP -> {
                releaseLocks()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                if (title.isNullOrBlank()) {
                    // Nothing playing, tear down foreground
                    releaseLocks()
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentTitle = title
                currentArtist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                currentAlbum = intent.getStringExtra(EXTRA_ALBUM) ?: ""
                currentFormat = intent.getStringExtra(EXTRA_FORMAT) ?: "STEREO"
                currentAccent = intent.getLongExtra(EXTRA_ACCENT, 0xFF00FF66L)
                isPlayingState = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                isLikedState = intent.getBooleanExtra(EXTRA_IS_LIKED, false) || (isLikedProvider?.invoke() == true)

                updateForeground(isPlayingState)
                return START_STICKY
            }
        }

        return START_STICKY
    }

    private fun updateForeground(isPlaying: Boolean) {
        if (currentTitle.isBlank()) return

        val notification = buildNotification(isPlaying)

        if (isPlaying) {
            acquireLocks()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (_: Exception) {
                try {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
            }
        } else {
            releaseLocks()
            try {
                // Keep notification in drawer on pause so user can resume anytime
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {
            }
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun buildPendingServiceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, BrokeifyPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPendingIntent = buildPendingServiceIntent(ACTION_PREV, 201)
        val playPausePendingIntent = buildPendingServiceIntent(ACTION_PLAY_PAUSE, 202)
        val nextPendingIntent = buildPendingServiceIntent(ACTION_NEXT, 203)
        val likePendingIntent = buildPendingServiceIntent(ACTION_TOGGLE_LIKE, 204)

        val artwork = generateArtworkBitmap(currentTitle, currentArtist, currentAccent)
        val isLiked = isLikedState || (isLikedProvider?.invoke() == true)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setLargeIcon(artwork)
            .setContentTitle(currentTitle)
            .setContentText("$currentArtist • $currentAlbum")
            .setSubText("Brokeify Docker ($currentFormat)")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            )
            .addAction(
                if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off,
                if (isLiked) "Liked ❤️" else "Like 🤍",
                likePendingIntent
            )

        try {
            mediaSession?.let { session ->
                val mediaStyle = androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
                builder.setStyle(mediaStyle)
            }
        } catch (_: Exception) {
        }

        return builder.build()
    }

    private fun generateArtworkBitmap(title: String, artist: String, accentHex: Long): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (0xFF000000L or (accentHex and 0x00FFFFFFL)).toInt()
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // Vinyl grooves
        val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(50, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.38f, groovePaint)
        canvas.drawCircle(size / 2f, size / 2f, size * 0.28f, groovePaint)

        // Center label
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(230, 7, 10, 15)
        }
        canvas.drawCircle(size / 2f, size / 2f, size * 0.18f, centerPaint)

        // Center note icon
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 52f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♪", size / 2f, size / 2f + 18f, textPaint)

        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        try {
            mediaSession?.release()
            mediaSession = null
        } catch (_: Exception) {
        }
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
