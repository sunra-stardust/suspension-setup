package dev.suspension.app.update

import android.content.Context

/** Google Play build: no self-updater (Play policy) — Play delivers updates. */
object Distribution {
    fun updater(context: Context): Updater? = null

    fun storeUrl(context: Context): String? = "https://play.google.com/store/apps/details?id=${context.packageName}"
}
