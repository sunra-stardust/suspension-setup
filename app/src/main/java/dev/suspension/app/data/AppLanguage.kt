package dev.suspension.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The rider's language choice. [tag] is what gets stored — additive key, so an older release
 * simply ignores it and follows the device language.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    GERMAN("de"),
    ;

    /** Null = follow the device. */
    val locale: Locale? get() = if (this == SYSTEM) null else Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * Stores the language in SharedPreferences (not DataStore): the activity needs it synchronously
 * in `attachBaseContext`, before anything is drawn.
 */
object LanguageStore {
    private const val PREFS = "app_language"
    private const val KEY = "language"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): AppLanguage = AppLanguage.fromTag(prefs(context).getString(KEY, null))

    fun set(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY, language.tag).commit()
    }

    /** [base] with its resources switched to the chosen language; unchanged for [AppLanguage.SYSTEM]. */
    fun wrap(base: Context): Context {
        val locale = get(base).locale ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
