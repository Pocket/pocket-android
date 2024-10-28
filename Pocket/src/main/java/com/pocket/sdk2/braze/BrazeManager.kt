package com.pocket.sdk2.braze

import android.content.Context
import com.braze.Braze
import com.braze.BrazeActivityLifecycleCallbackListener
import com.braze.models.inappmessage.IInAppMessage
import com.braze.models.inappmessage.IInAppMessageThemeable
import com.braze.ui.inappmessage.BrazeInAppMessageManager
import com.braze.ui.inappmessage.InAppMessageOperation
import com.braze.ui.inappmessage.listeners.IInAppMessageManagerListener
import com.pocket.app.*
import com.pocket.app.add.AddActivity
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.settings.Theme
import com.pocket.sdk.tts.ListenDeepLinkActivity
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.util.android.PPActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrazeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLifecycleEventDispatcher: AppLifecycleEventDispatcher,
    private val pocketCache: PocketCache,
    private val theme: Theme,
) : AppLifecycle, IInAppMessageManagerListener {

    fun setup() {
        (context as App).registerActivityLifecycleCallbacks(
            BrazeActivityLifecycleCallbackListener(
                sessionHandlingEnabled = true,
                registerInAppMessageManager = true,
                inAppMessagingRegistrationBlocklist = hashSetOf(
                    AuthenticationActivity::class.java,
                    PocketUrlHandlerActivity::class.java,
                    AddActivity::class.java,
                    AppCacheCheckActivity::class.java,
                    ListenDeepLinkActivity::class.java,
                    PPActivity::class.java,
                )
            )
        )
        BrazeInAppMessageManager.getInstance().setCustomInAppMessageManagerListener(this)

        appLifecycleEventDispatcher.registerAppLifecycleObserver(this)
        setUserId()
    }

    override fun onLoggedIn(isNewUser: Boolean) {
        super.onLoggedIn(isNewUser)
        setUserId()
    }

    private fun setUserId() {
        if (!pocketCache.isLoggedIn) return

        pocketCache.uid?.let { Braze.getInstance(context).changeUser(it) }

    }

    override fun beforeInAppMessageDisplayed(inAppMessage: IInAppMessage): InAppMessageOperation {
        if (inAppMessage is IInAppMessageThemeable && theme.isDark(context)) {
            inAppMessage.enableDarkTheme()
        }
        return InAppMessageOperation.DISPLAY_NOW
    }
}