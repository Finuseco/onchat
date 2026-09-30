package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real Voice Call Audio Engine
 * Captures real microphone input using Android AudioRecord with hardware Echo Cancellation & Noise Suppression,
 * and plays incoming voice audio using AudioTrack via VOICE_COMMUNICATION stream.
 */
class RealVoiceCallManager(private val context: Context) {

    companion object {
        private const val TAG = "RealVoiceCallManager"
        private const val SAMPLE_RATE = 16000 // 16 kHz HD Voice
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        @Volatile
        private var instance: RealVoiceCallManager? = null

        fun getInstance(context: Context): RealVoiceCallManager {
            return instance ?: synchronized(this) {
                instance ?: RealVoiceCallManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)
    private val isMuted = AtomicBoolean(false)
    private val isSpeakerOn = AtomicBoolean(true)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var captureJob: Job? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Queue for incoming voice packets
    private val incomingAudioQueue = ConcurrentLinkedQueue<ByteArray>()

    // Callback when local voice chunk is recorded
    var onLocalAudioChunk: ((ByteArray) -> Unit)? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Start duplex voice session
     */
    fun startVoiceSession() {
        if (isRecording.get()) return

        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = isSpeakerOn.get()
                am.isMicrophoneMute = false
            }

            val minBufferSizeIn = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT
            ).coerceAtLeast(1024)

            val minBufferSizeOut = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT
            ).coerceAtLeast(1024)

            // Setup AudioRecord
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                minBufferSizeIn * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            // Enable Hardware Acoustic Echo Cancellation & Noise Suppression if supported
            if (AcousticEchoCanceler.isAvailable()) {
                try {
                    echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
                    echoCanceler?.enabled = true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not enable EchoCanceler", e)
                }
            }

            if (NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
                    noiseSuppressor?.enabled = true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not enable NoiseSuppressor", e)
                }
            }

            // Setup AudioTrack
            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG_OUT)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSizeOut * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_OUT,
                    AUDIO_FORMAT,
                    minBufferSizeOut * 2,
                    AudioTrack.MODE_STREAM
                )
            }

            audioRecord = record
            audioTrack = track

            isRecording.set(true)
            isPlaying.set(true)

            record.startRecording()
            track.play()

            // Coroutine 1: Capture Mic Audio and forward
            captureJob = scope.launch {
                val buffer = ByteArray(minBufferSizeIn)
                while (isRecording.get() && isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (!isMuted.get()) {
                            val chunk = buffer.copyOf(read)
                            onLocalAudioChunk?.invoke(chunk)
                        }
                    }
                }
            }

            // Coroutine 2: Playback incoming voice audio
            playbackJob = scope.launch {
                while (isPlaying.get() && isActive) {
                    val chunk = incomingAudioQueue.poll()
                    if (chunk != null && chunk.isNotEmpty()) {
                        track.write(chunk, 0, chunk.size)
                    } else {
                        delay(10)
                    }
                }
            }

            Log.i(TAG, "Voice session started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice session", e)
            stopVoiceSession()
        }
    }

    /**
     * Push incoming remote audio packet for playback
     */
    fun onIncomingRemoteAudio(data: ByteArray) {
        if (isPlaying.get() && data.isNotEmpty()) {
            incomingAudioQueue.offer(data)
        }
    }

    /**
     * Mute or Unmute local microphone
     */
    fun setMute(muted: Boolean) {
        isMuted.set(muted)
        try {
            audioManager?.isMicrophoneMute = muted
        } catch (_: Exception) {}
    }

    /**
     * Toggle Loudspeaker / Earpiece
     */
    fun setSpeakerOn(speakerOn: Boolean) {
        isSpeakerOn.set(speakerOn)
        try {
            audioManager?.let { am ->
                am.isSpeakerphoneOn = speakerOn
            }
        } catch (_: Exception) {}
    }

    /**
     * Stop voice session and release hardware resources
     */
    fun stopVoiceSession() {
        isRecording.set(false)
        isPlaying.set(false)

        captureJob?.cancel()
        playbackJob?.cancel()

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioRecord = null

        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
        } catch (_: Exception) {}
        echoCanceler = null
        noiseSuppressor = null

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null

        incomingAudioQueue.clear()

        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_NORMAL
                am.isSpeakerphoneOn = false
            }
        } catch (_: Exception) {}

        Log.i(TAG, "Voice session stopped")
    }
}
