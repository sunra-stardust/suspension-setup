package dev.suspension.app.update

import android.content.Context
import dev.suspension.app.BuildConfig

/** GitHub build: updates itself from GitHub Releases. */
object Distribution {
    fun updater(context: Context): Updater? = GitHubUpdater(context.applicationContext, BuildConfig.UPDATE_REPO)

    /** Only the Play build points people to a store page. */
    fun storeUrl(context: Context): String? = null
}
