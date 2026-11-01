package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceCompanion(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    private val _offlineAvailable = MutableStateFlow(false)
    val offlineAvailable = _offlineAvailable.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val voice = tts?.voices?.filter {
                !it.isNetworkConnectionRequired && it.locale.language == "en" &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
            }?.sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.name })?.firstOrNull()
            if (voice != null && tts?.setVoice(voice) == TextToSpeech.SUCCESS) {
                isInitialized = true
                _offlineAvailable.value = true
                tts?.setSpeechRate(0.92f) // Slightly slower, calm cadence
                tts?.setPitch(0.95f) // Warm, grounded pitch
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String): Boolean {
        val engine = tts ?: return false
        val voice = engine.voice ?: return false
        if (!isInitialized || !_offlineAvailable.value || voice.isNetworkConnectionRequired ||
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in voice.features.orEmpty()) return false
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "newme_voice_id")
        _isSpeaking.value = result == TextToSpeech.SUCCESS
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isSpeaking.value = false
        _offlineAvailable.value = false
    }
}
