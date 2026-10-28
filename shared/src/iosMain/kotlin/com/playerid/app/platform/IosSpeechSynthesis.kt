package com.playerid.app.platform

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityDefault
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityEnhanced
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityPremium
import platform.AVFAudio.AVSpeechSynthesisVoiceTraitIsNoveltyVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifier

/**
 * Speaks short roster answers through the app's current audio session.
 * Uses the most natural installed voice: Premium, then Enhanced, then the default compact voice.
 * Better voices are downloaded by the user in Settings > Accessibility > Spoken Content > Voices.
 */
class IosSpeechSynthesis : SpeechSynthesisService {
    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        if (text.isBlank()) return
        stop()
        val utterance = AVSpeechUtterance(string = text).apply {
            rate = AVSpeechUtteranceDefaultSpeechRate * 0.96f
            pitchMultiplier = 1.0f
            voice = bestVoice()
        }
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        if (synthesizer.isSpeaking()) synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    private fun bestVoice(): AVSpeechSynthesisVoice? {
        val locale = NSLocale.currentLocale
        val region = locale.localeIdentifier.replace('_', '-')
        val language = locale.languageCode
        val candidates = AVSpeechSynthesisVoice.speechVoices().filterIsInstance<AVSpeechSynthesisVoice>()
            .filter { it.language.startsWith(language) && (it.voiceTraits and AVSpeechSynthesisVoiceTraitIsNoveltyVoice) == 0uL }
        return candidates.maxWithOrNull(
            compareBy<AVSpeechSynthesisVoice>({ qualityRank(it) }, { it.language == region })
        ) ?: AVSpeechSynthesisVoice.voiceWithLanguage(language)
    }

    private fun qualityRank(voice: AVSpeechSynthesisVoice): Int = when (voice.quality) {
        AVSpeechSynthesisVoiceQualityPremium -> 3
        AVSpeechSynthesisVoiceQualityEnhanced -> 2
        AVSpeechSynthesisVoiceQualityDefault -> 1
        else -> 0
    }
}
