package com.pocket.sdk.notification.push

import android.content.Context
import com.pocket.sdk.Pocket
import com.pocket.sdk.api.AppSync
import com.pocket.app.AppThreads
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.app.AppMode
import com.pocket.util.prefs.StringPreference
import com.pocket.util.prefs.BooleanPreference
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.messaging.FirebaseMessaging
import com.pocket.sdk.api.generated.enums.CxtUi
import com.pocket.util.java.Logs
import com.pocket.sdk2.analytics.context.Interaction
import com.pocket.sdk.api.generated.thing.ActionContext
import com.pocket.sdk.api.generated.enums.PushType
import com.pocket.sync.source.result.SyncException
import com.pocket.sync.thing.Thing
import com.pocket.util.prefs.Preferences

/**
 * Handles registering and deregistering from receiving device push notifications.
 * Pocket uses Firebase Messaging in order to receive notifications.
 */
class PktPush(
    private val context: Context,
    private val pocket: Pocket,
    appSync: AppSync,
    private val appThreads: AppThreads,
    private val pktCache: PocketCache,
    prefs: Preferences,
    private val mode: AppMode
) : Push {

    private val registeredGuid: StringPreference = prefs.forApp("registeredGuidFirebase", null as String?)
    private val fcmToken: StringPreference = prefs.forApp("dev_pref_fcm_token", null as String?)
    private val reregister: BooleanPreference = prefs.forApp("reregisterFirebase", false)

    init {
        // If we need to get a new token, do it on next app sync, which marks a good time for the app to do network activity
        appSync.addWork(Runnable {
            if (reregister.get()) {
                register(null, null)
            }
        })
    }

    override fun getToken(): String? = fcmToken.get()

    override fun isAvailable(): Boolean {
        // TODO some statuses are recoverable, use GoogleApiAvailability.getErrorDialog to handle displaying a message to the user,
        // which will require an Activity context to show and a request code / onActivityResult path to handle retrying
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS &&  // play services available
                pktCache.isLoggedIn // user is logged in
    }

    override fun register(cxt_ui: CxtUi?, registrationListener: Push.RegistrationListener?) {
        if (!isAvailable) {
            registrationListener?.onResult(false, null)
            return
        }

        // done asynchronously in order to retrieve the Firebase token and user guid
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            appThreads.async {
                val guid = try {
                    pocket.sync(pocket.spec().things().loginInfo().build()).get().guid
                } catch (e: SyncException) {
                    Logs.printStackTrace(e)
                    null
                }
                if (guid != null) {
                    val i = Interaction.on(context)
                        .merge { cxt: ActionContext.Builder -> cxt.cxt_ui(cxt_ui) }
                    pocket.syncRemote<Thing?>(
                        null, pocket.spec().actions().register_push()
                            .context(i.context)
                            .time(i.time)
                            .device_identifier(guid)
                            .token(token)
                            .push_type(PushType.PRODUCTION)
                            .build()
                    )
                        .onFailure { e: SyncException ->
                            registrationListener?.onResult(
                                false,
                                e.userFacingMessage
                            )
                        }
                        .onSuccess {
                            registeredGuid.set(guid)
                            if (mode.isForInternalCompanyOnly) fcmToken.set(token)
                            reregister.set(false)
                            registrationListener?.onResult(true, null)
                        }
                } else {
                    appThreads.runOrPostOnUiThread { registrationListener?.onResult(false, null) }
                }
            }
        }.addOnFailureListener {
            appThreads.runOrPostOnUiThread { registrationListener?.onResult(false, null) }
        }
    }

    override fun deregister(cxt_ui: CxtUi) {
        if (!isAvailable) return
        reregister.set(false)
        if (registeredGuid.get() != null) {
            val i = Interaction.on(context).merge { cxt: ActionContext.Builder -> cxt.cxt_ui(cxt_ui) }
            pocket.syncRemote<Thing?>(
                null, pocket.spec().actions().deregister_push()
                    .context(i.context)
                    .time(i.time)
                    .device_identifier(registeredGuid.get())
                    .push_type(PushType.PRODUCTION)
                    .build()
            ).onSuccess { registeredGuid.set(null) }
        }
    }

    override fun invalidate() {
        reregister.set(true)
        register(null, null)
    }
}