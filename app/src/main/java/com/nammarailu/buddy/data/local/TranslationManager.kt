package com.nammarailu.buddy.data.local

import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * TranslationManager — translates all dynamic Firebase data (train names, station names,
 * status text, etc.) using Google ML Kit offline translation.
 *
 * - Works 100% offline after the model is downloaded (~30MB per language)
 * - Caches every translation in memory so the same word is never translated twice
 * - Models are downloaded automatically on first use
 */
@Singleton
class TranslationManager @Inject constructor() {

    // In-memory cache: "text|langCode" -> translated text
    private val cache = mutableMapOf<String, String>()

    // Keep translators alive to avoid re-init overhead
    private val translators = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    // Hardcoded dictionary for local Coastal Karnataka station names
    // ML Kit often transliterates these poorly or leaves them in English.
    private val hardcodedTranslations = mapOf(
        "Kumta" to mapOf("kn" to "ಕುಮಟಾ", "hi" to "कुमता"),
        "Byndoor" to mapOf("kn" to "ಬೈಂದೂರು", "hi" to "बयंदूर"),
        "Murdeshwar" to mapOf("kn" to "ಮುರುಡೇಶ್ವರ", "hi" to "मुरुडेश्वर"),
        "Udupi" to mapOf("kn" to "ಉಡುಪಿ", "hi" to "उडुपी"),
        "Mangaluru Central" to mapOf("kn" to "ಮಂಗಳೂರು ಸೆಂಟ್ರಲ್", "hi" to "मंगलूरु सेंट्रल"),
        "Mangaluru Junction" to mapOf("kn" to "ಮಂಗಳೂರು ಜಂಕ್ಷನ್", "hi" to "मंगलूरु जंक्शन"),
        "Karwar" to mapOf("kn" to "ಕಾರವಾರ", "hi" to "कारवार"),
        "Bhatkal" to mapOf("kn" to "ಭಟ್ಕಳ", "hi" to "भटकल"),
        "Gokarna Road" to mapOf("kn" to "ಗೋಕರ್ಣ ರಸ್ತೆ", "hi" to "गोकर्ण रोड"),
        "Kundapura" to mapOf("kn" to "ಕುಂದಾಪುರ", "hi" to "कुंदापुरा")
    )

    /**
     * Translate [text] from English to [targetLanguage] ("kn" or "hi").
     * Returns original text if language is "en" or translation fails.
     */
    suspend fun translate(text: String, targetLanguage: String): String {
        if (targetLanguage == "en" || text.isBlank()) return text

        // Check hardcoded exact matches first for known local stations
        hardcodedTranslations[text]?.get(targetLanguage)?.let { return it }

        val cacheKey = "$text|$targetLanguage"
        cache[cacheKey]?.let { return it }

        val mlkitLang = when (targetLanguage) {
            "kn" -> TranslateLanguage.KANNADA
            "hi" -> TranslateLanguage.HINDI
            else -> return text
        }

        return try {
            val translator = translators.getOrPut(targetLanguage) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(mlkitLang)
                    .build()
                Translation.getClient(options)
            }

            // Download model if not already on device
            suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded()
                    .addOnSuccessListener {
                        translator.translate(text)
                            .addOnSuccessListener { translated ->
                                cache[cacheKey] = translated
                                cont.resume(translated)
                            }
                            .addOnFailureListener { e ->
                                Log.w("TranslationManager", "Translation failed for '$text': ${e.message}")
                                cont.resume(text)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.w("TranslationManager", "Model download failed: ${e.message}")
                        cont.resume(text)
                    }
            }
        } catch (e: Exception) {
            Log.e("TranslationManager", "Unexpected error: ${e.message}")
            text
        }
    }

    /** Translate a list of texts in one call. Efficient for translating whole train/station lists. */
    suspend fun translateAll(texts: List<String>, targetLanguage: String): List<String> {
        if (targetLanguage == "en") return texts
        return texts.map { translate(it, targetLanguage) }
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}
