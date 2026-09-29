package dev.suspension.app

import android.app.Application
import dev.suspension.app.safety.CrashGuard

class SuspensionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
    }
}
