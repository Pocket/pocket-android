package com.pocket.app.notes

import com.pocket.app.AppMode
import com.pocket.sdk.api.ServerFeatureFlags
import com.pocket.sync.await
import javax.inject.Inject
import javax.inject.Singleton

private const val KillSwitch = "perm.android.app.notes.kill_switch"
private const val DevelopmentFlag = "temp.android.app.notes.dev"

@Singleton
class Notes
@Inject constructor(
    private val flags: ServerFeatureFlags,
    private val mode: AppMode,
) {
    suspend fun areEnabled(): Boolean {
        if (!mode.isForInternalCompanyOnly) return false
        if (flags.get(DevelopmentFlag).await()?.assigned != true) return false
        if (flags.get(KillSwitch).await()?.assigned == true) return false
        return true
    }
}
