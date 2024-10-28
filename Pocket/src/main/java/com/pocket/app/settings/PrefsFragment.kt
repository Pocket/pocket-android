package com.pocket.app.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ideashower.readitlater.BuildConfig
import com.ideashower.readitlater.R
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SettingsEvents
import com.pocket.app.App
import com.pocket.app.AppMode
import com.pocket.app.CustomTabs
import com.pocket.app.SaveExtension
import com.pocket.app.UserManager
import com.pocket.app.auth.AuthenticationActivity
import com.pocket.app.reader.Reader
import com.pocket.app.settings.account.AccountManagementFragment
import com.pocket.app.settings.appicon.AppIcons
import com.pocket.app.settings.beta.TCActivity
import com.pocket.app.settings.cache.CacheSettingsFragment
import com.pocket.app.settings.premium.PremiumSettingsFragment
import com.pocket.app.settings.view.preferences.ActionPreference
import com.pocket.app.settings.view.preferences.MultipleChoicePreference.OnSelectedItemChangedListener
import com.pocket.app.settings.view.preferences.Preference
import com.pocket.app.settings.view.preferences.PreferenceViews
import com.pocket.app.settings.view.preferences.RingtonePreference
import com.pocket.app.settings.view.preferences.ToggleSwitchPreference
import com.pocket.sdk.api.AppSync
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier
import com.pocket.sdk.notification.SystemNotifications
import com.pocket.sdk.offline.cache.Assets
import com.pocket.sdk.offline.cache.StorageLocationPickerDialog
import com.pocket.sdk.preferences.AppPrefs
import com.pocket.sdk.util.AbsPocketActivity
import com.pocket.sdk.util.DeepLinks
import com.pocket.sdk.util.dialog.AlertMessaging
import com.pocket.sdk.util.dialog.ProgressDialogFragment
import com.pocket.sdk.util.file.AndroidStorageUtil
import com.pocket.sdk.util.service.BackgroundSync
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.sync.source.subscribe.Subscription
import com.pocket.util.android.ApiLevel
import com.pocket.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PrefsFragment : AbsPrefsFragment() {
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var saveExtension: SaveExtension
    @Inject lateinit var appIcons: AppIcons
    @Inject lateinit var customTabs: CustomTabs
    @Inject lateinit var reader: Reader
    @Inject lateinit var assets: Assets
    @Inject lateinit var appSync: AppSync
    @Inject lateinit var backgroundSync: BackgroundSync
    @Inject lateinit var notifications: SystemNotifications
    @Inject lateinit var mode: AppMode
    @Inject lateinit var tracker: Tracker
    @Inject lateinit var appPrefs: AppPrefs
    @Inject lateinit var pocketCache: PocketCache

    private val mDisposeInOnStop = CompositeDisposable()
    private var mUsernameSubscription: Subscription? = null
    private var mPremiumSubscription: Subscription? = null
    private var mRingtonePreference: RingtonePreference? = null
    private var browserPreferenceEnabled = true

    override fun getScreenIdentifier(): UiEntityIdentifier? {
        return UiEntityIdentifier.SETTINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = false
    }

    override fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        super.onViewCreatedImpl(view, savedInstanceState)
        appbar.leftIcon.visibility = View.GONE
        list.itemAnimator = null // Disable flicker when rebuilding from repeatOnLifecycle.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                browserPreferenceEnabled = customTabs.isBrowserSettingEnabled()
                rebuildPrefs()
            }
        }
    }

    override fun createPrefs(prefs: ArrayList<Preference>) {
        val activity = activity as AbsPocketActivity?
        val isLoggedIn = pocketCache.isLoggedIn

        addAlphaSettings(prefs)

        // Account
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_account, prefs.size > 0))

        if (isLoggedIn) {
            // Premium
            prefs.add(PreferenceViews.newActionBuilder(this, R.string.setting_premium)
                .setOnClickListener { PremiumSettingsFragment.show(absPocketActivity, null) }
                .build())

            // Account management (delete account)
            prefs.add(PreferenceViews.newActionBuilder(this, R.string.setting_account_management)
                .setOnClickListener {
                    tracker.track(SettingsEvents.accountManagementRowClicked())
                    AccountManagementFragment.show(absPocketActivity, null)
                }
                .build())

            // Log Out
            prefs.add(PreferenceViews.newImportantBuilder(this, R.string.setting_logout)
                .setOnClickListener {
                    tracker.track(SettingsEvents.logoutRowClicked())
                    AlertMessaging.show(
                        activity,
                        R.string.dg_confirm_t,
                        R.string.dg_confirm_logout_m,
                        R.string.ac_cancel,
                        null,
                        R.string.ac_logout
                    ) { _, _ ->
                        tracker.track(SettingsEvents.logoutConfirmClicked())
                        userManager.logout(absPocketActivity)
                    }
                }
                .build())
        } else {
            // Sign up or sign in
            prefs.add(PreferenceViews.newActionBuilder(this, R.string.ac_authenticate)
                .setOnClickListener {
                    tracker.track(SettingsEvents.loginRowClicked())
                    AuthenticationActivity.startActivity(requireContext(), true)
                }
                .build())
        }


        if (isLoggedIn) {
            // General
            prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_general))


            // App Rotation Lock
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appPrefs.ROTATION_LOCK,
                    R.string.setting_rotation_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_rotation_sum)
                    .build()
            )

            // Add overlay
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    object : ToggleSwitchPreference.PrefHandler {
                        override fun get() = saveExtension.isOn
                        override fun set(value: Boolean) { saveExtension.isOn = value }
                    },
                    R.string.setting_share_overlay_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_share_overlay_sum)
                    .setIdentifier(UiEntityIdentifier.SETTING_SAVE_EXTENSION)
                    .build()
            )
        }

        // Theme
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_theme))

        val systemDarkTheme = app().systemDarkTheme()
        prefs.add(
            PreferenceViews.newMultipleChoiceBuilder(
                this,
                app().theme().pref(),
                R.string.setting_theme
            )
                .addChoice(R.string.nm_theme_light)
                .addChoice(R.string.nm_theme_dark)
                .setOnItemSelectedListener(object : OnSelectedItemChangedListener {
                    override fun onItemSelected(
                        view: View,
                        newValue: Int,
                        dialog: DialogInterface
                    ): Boolean {
                        // called here in onItemSelected in order to change theme before the PrefKey gets changed
                        systemDarkTheme.turnOff(view)
                        app().displaySettings().setTheme(view, newValue)
                        return true
                    }

                    override fun onItemSelectionChanged(newValue: Int) {
                        // unused, theme is now changed
                    }
                })
                .setIdentifier(UiEntityIdentifier.SETTING_CHANGE_THEME)
                .build()
        )
        // Follow system theme
        if (systemDarkTheme.isEnabled) {
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    systemDarkTheme.preference,
                    R.string.setting_system_theme_label
                )
                    .setOnChangeListener(object : ToggleSwitchPreference.OnChangeListener {
                        override fun onChange(view: View, nowEnabled: Boolean): Boolean {
                            if (nowEnabled) {
                                systemDarkTheme.turnOn(view)
                            } else {
                                systemDarkTheme.turnOff(view)
                            }


                            // return false because we've handled the change above in order to 
                            // properly trigger analytics events
                            return false
                        }

                        override fun afterChange(nowEnabled: Boolean) {}
                    })
                    .setIdentifier(UiEntityIdentifier.SETTING_SYSTEM_THEME)
                    .build()
            )
        }

        prefs.add(
            PreferenceViews.newActionBuilder(
                this,
                R.string.setting_app_icon_label,
            )
                .setOnClickListener {
                    tracker.track(SettingsEvents.appIconRowClicked())
                    findNavController().navigateSafely(PrefsFragmentDirections.goToAppIconSettings())
                }
                .setSummaryDefaultUnchecked(appIcons.current.label)
                .build()
        )

        // Reading
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_reading))

        if (isLoggedIn) {
            // Open Original Website
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appPrefs.ALWAYS_OPEN_ORIGINAL,
                    R.string.setting_always_open_original_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_always_open_original_sum)
                    .build()
            )
        }

        // Open Custom Tabs with
        if (browserPreferenceEnabled) {
            val options = customTabs.getBrowserOptions()
            prefs.add(PreferenceViews.newActionBuilder(this, R.string.setting_default_browser_label)
                .setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.setting_default_browser_label)
                        .setSingleChoiceItems(
                            options.labels,
                            options.selected
                        ) { dialog: DialogInterface, which: Int ->
                            options.onSelected(which)
                            dialog.dismiss()
                            rebuildPrefs()
                        }
                        .show()
                }
                .setSummaryDefaultUnchecked(options.selectedLabel.toString())
                .build())
        }

        // Previous and next
        prefs.add(
            PreferenceViews.newToggleSwitchBuilder(
                this,
                object : ToggleSwitchPreference.PrefHandler {
                    override fun get(): Boolean = reader.isPreviousAndNextOn
                    override fun set(value: Boolean) { reader.isPreviousAndNextOn = value }
                },
                R.string.setting_previous_and_next
            )
                .setSummaryDefaultUnchecked(R.string.previous_and_next_setting_description)
                .build()
        )

        // Text Alignment
        prefs.add(PreferenceViews.newToggleSwitchBuilder(
            this,
            app().displaySettings().justify(),
            R.string.setting_text_align_label
        )
            .setSummaryDefaultUnchecked(R.string.setting_text_align_sum)
            .setOnChangeListener { n: Boolean ->
                app().displaySettings()
                    .onJustificationSettingChanged()
            } // TODO the display settings manager should handle listening for this itself.
            .setIdentifier(UiEntityIdentifier.SETTING_JUSTIFICATION)
            .build())

        // Auto Fullscreen
        prefs.add(
            PreferenceViews.newToggleSwitchBuilder(
                this,
                appPrefs.READER_AUTO_FULLSCREEN,
                R.string.setting_auto_fullscreen_label
            )
                .setSummaryDefaultUnchecked(R.string.setting_auto_fullscreen_sum)
                .build()
        )

        if (isLoggedIn) {
            // Continue Reading
            prefs.add(PreferenceViews.newToggleSwitchBuilder(
                this,
                appPrefs.CONTINUE_READING_ENABLED,
                R.string.setting_continue_reading_label
            )
                .setSummaryDefaultUnchecked(R.string.setting_continue_reading_sum)
                .setIdentifier(UiEntityIdentifier.SETTING_CONTINUE_READING)
                .build())
        }

        if (isLoggedIn) {
            // Offline Downloading
            prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_offline))


            // Always Fetch Article
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appPrefs.DOWNLOAD_TEXT,
                    R.string.setting_download_article_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_download_article_sum)
                    .setSummaryDisabled(R.string.setting_download_article_sum_unavailable)
                    .build()
            )


            // Download Only on Wi-Fi
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appPrefs.DOWNLOAD_ONLY_WIFI,
                    R.string.setting_only_wifi_label
                )
                    .build()
            )


            // Mobile User-Agent
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appPrefs.USE_MOBILE_AGENT,
                    R.string.setting_mobile_useragent_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_mobile_useragent_sum)
                    .build()
            )


            // Storage Location
            prefs.add(
                PreferenceViews.newActionBuilder(this, R.string.setting_storage_location)
                    .setSummaryDefaultUnchecked(
                        AndroidStorageUtil.getStorageLocationSummary(
                            assets.storageType,
                            getActivity()
                        )
                    )
                    .setOnClickListener(object : ActionPreference.OnClickAction {
                        override fun onClick() {
                            StorageLocationPickerDialog.show(getActivity()) { onClick() }
                            // If changed, prefs will be rebuilt. see onSharedPreferencesChanged()
                        }
                    })
                    .build()
            )

            // Manage Cache Limits
            prefs.add(PreferenceViews.newActionBuilder(
                this,
                R.string.setting_cache_set_offline_storage_limits
            )
                .setOnClickListener { CacheSettingsFragment.show(getActivity()) }
                .build())


            // Clear Downloaded Files
            prefs.add(PreferenceViews.newActionBuilder(this, R.string.setting_clear_download_label)
                .setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.dg_clear_cache_t)
                        .setMessage(R.string.dg_clear_cache_m)
                        .setNegativeButton(R.string.ac_cancel, null)
                        .setPositiveButton(R.string.ac_clear_cache) { _, _ ->
                            val progress =
                                ProgressDialogFragment.getNew(R.string.dg_clearing_cache, false)
                            progress.showOnCurrentActivity()
                            assets.clearOfflineContent({
                                viewLifecycleOwner.lifecycleScope
                                    .launch { progress.dismissAllowingStateLoss() }
                            }, null)
                        }
                        .show()
                }
                .build())
        }

        if (isLoggedIn) {
            // Syncing
            prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_syncing))

            // Auto sync on open
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    appSync.autoSyncPref(),
                    R.string.setting_sync_on_open_label
                )
                    .build()
            )

            // Background syncing
            val backgroundSyncingPref = PreferenceViews.newMultipleChoiceBuilder(
                this,
                backgroundSync.pref(),
                R.string.setting_background_sync_label
            )
                .addChoice(R.string.setting_background_sync_0)
                .addChoice(R.string.setting_background_sync_1)
                .addChoice(R.string.setting_background_sync_2)
                .addChoice(R.string.setting_background_sync_3)
                .addChoice(R.string.setting_background_sync_4)
                .setOnItemSelectedListener(object : OnSelectedItemChangedListener {
                    override fun onItemSelected(
                        view: View,
                        newValue: Int,
                        dialog: DialogInterface?
                    ): Boolean {
                        OptionsDialogs.backgroundSyncingChange(newValue, { result: Boolean ->
                            if (result) {
                                dialog?.dismiss()
                            }
                        }, activity)

                        if (newValue == BackgroundSync.SYNC_INSTANT) {
                            // Will attempt to setup c2dm, and if successful it will change the sync setting itself
                            return false
                        } else {
                            dialog?.dismiss()
                            return true
                        }
                    }

                    override fun onItemSelectionChanged(newValue: Int) {
                    }
                })
            if (mode.isForInternalCompanyOnly) {
                backgroundSyncingPref.addChoice(R.string.setting_background_sync_5)
            }
            prefs.add(backgroundSyncingPref.build())
        }

        // Notification Settings
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_notifications))

        if (ApiLevel.isOreoOrGreater()) {
            // Notification Channels are managed in OS systems now

            prefs.add(PreferenceViews.newActionBuilder(
                this,
                R.string.setting_manage_notification_settings
            )
                .setSummaryDefaultUnchecked(R.string.setting_manage_notification_settings_sum)
                .setOnClickListener {
                    val intent = DeepLinks.newDeviceNotificationSettingsIntent(context)
                    startActivity(intent)
                }
                .build())
        } else {
            // Notification Sound
            mRingtonePreference = RingtonePreference(
                this,
                notifications.sound,
                getString(R.string.setting_notify_sound_label),
                { true }, null
            )
            prefs.add(mRingtonePreference!!)


            // LED
            prefs.add(
                PreferenceViews.newToggleSwitchBuilder(
                    this,
                    notifications.lights,
                    R.string.setting_notify_light_label
                )
                    .setSummaryDefaultUnchecked(R.string.setting_notify_light_sum)
                    .build()
            )
        }


        // About
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_about))

        // Help
        prefs.add(
            newOpenUrlPref(
                R.string.setting_help,
                "https://help.getpocket.com/",
            )
        )

        // Legal & Privacy
        prefs.add(
            newOpenUrlPref(
                R.string.setting_tos,
                "https://getpocket.com/en/tos/",
            )
        )
        prefs.add(
            newOpenUrlPref(
                R.string.setting_privacy,
                "https://getpocket.com/en/privacy/",
            )
        )
        prefs.add(PreferenceViews.newActionBuilder(this, R.string.setting_oss)
            .setOnClickListener {
                findNavController().navigateSafely(PrefsFragmentDirections.goToOpenSourceLicenses())
            }
            .build())

        // Contact Us
        prefs.add(
            newOpenUrlPref(
                R.string.setting_contact_us,
                "https://getpocket.com/contact-info/"
            )
        )
        prefs.add(
            newOpenUrlPref(
                R.string.setting_twitter_label,
                "https://twitter.com/intent/user?screen_name=Pocket",
            )
        )
        prefs.add(
            newOpenUrlPref(
                R.string.setting_facebook_label,
                "https://facebook.com/readitlater",
            )
        )

        // Version
        prefs.add(PreferenceViews.newHeader(this, R.string.setting_header_version))

        prefs.add(
            PreferenceViews.newActionBuilder(this, getString(R.string.setting_version_label, BuildConfig.VERSION_NAME))
                .setSummaryDefaultUnchecked(R.string.setting_thank_you)
                .build()
        )

        if (mode.isForInternalCompanyOnly) {
            prefs.add(
                PreferenceViews.newActionBuilder(this, "Build Version")
                    .setSummaryDefaultUnchecked(BuildConfig.GIT_SHA)
                    .build()
            )
        }
    }

    private fun addAlphaSettings(prefs: ArrayList<Preference>) {
        if (!mode.isForInternalCompanyOnly) {
            return
        }

        prefs.add(PreferenceViews.newHeader(this, "Alpha only", false))
        prefs.add(PreferenceViews.newActionBuilder(this, R.string.alpha_settings)
            .setOnClickListener { startActivity(Intent(activity, TCActivity::class.java)) }
            .build())
    }

    private fun newOpenUrlPref(label: Int, url: String): Preference {
        return PreferenceViews.newActionBuilder(this, label)
            .setOnClickListener { App.viewUrl(activity, url) }
            .build()
    }

    override fun onStart() {
        super.onStart()
        rebuildPrefs() // Always make sure we have the latest settings if they left the screen and have returned.
        mDisposeInOnStop.add(
            assets.storageTypeChanges.subscribe { rebuildPrefs() }
        )
    }

    override fun onStop() {
        super.onStop()
        mUsernameSubscription = Subscription.stop(mUsernameSubscription)
        mPremiumSubscription = Subscription.stop(mPremiumSubscription)
        mDisposeInOnStop.clear()
    }

    override fun getTitle(): Int {
        return R.string.mu_settings
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mRingtonePreference!!.onActivityResult(requestCode, resultCode, data)
    }

    override fun getBannerView(): View? {
        return null
    }

    companion object {
        fun newInstance(): PrefsFragment {
            return PrefsFragment()
        }
    }
}
