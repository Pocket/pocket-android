package com.pocket.app.settings.premium;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.help.Help;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.Preference;
import com.pocket.app.settings.view.preferences.PreferenceViews;
import com.pocket.app.settings.view.preferences.PreferenceViews.ActionBuilder;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtSource;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.PremiumAllTimeStatus;
import com.pocket.sdk.api.generated.enums.PurchaseSource;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.api.generated.thing.PremiumFeatureStatus;
import com.pocket.sdk.api.generated.thing.PremiumSubscriptionInfo;
import com.pocket.sdk.api.generated.thing.PurchaseStatus;
import com.pocket.sdk.api.thing.AccountUtil;
import com.pocket.sdk.premium.billing.PremiumPurchaseHelper;
import com.pocket.sdk.premium.billing.PremiumPurchaseHelper.PurchaseListener;
import com.pocket.sdk.premium.billing.PremiumPurchaseHelper.PurchasingState;
import com.pocket.sdk.premium.billing.google.GoogleBillingUtil;
import com.pocket.sdk.premium.billing.google.Products;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.value.Parceller;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;

import java.util.ArrayList;
import java.util.List;

public class PremiumSettingsFragment extends AbsPrefsFragment {

	private static final String ARG_INFO = "info";
	
	public static FragmentLaunchMode getLaunchMode(Activity activity) {
		if (FormFactor.showSecondaryScreensInDialogs(activity)) {
			return FragmentLaunchMode.DIALOG;
		} else {
			return FragmentLaunchMode.ACTIVITY;
		}
	}
	
	public static PremiumSettingsFragment newInstance() {
		return new PremiumSettingsFragment();
	}
	
	/**
	 * @param activity
	 * @param mode
	 */
	public static void show(FragmentActivity activity, FragmentLaunchMode mode) {
		if (mode == null) {
			mode = getLaunchMode(activity);
		}
		if (mode == FragmentLaunchMode.DIALOG) {
			FragmentUtil.addFragmentAsDialog(newInstance(), activity);
		} else {
			PremiumSettingsActivity.startActivity(activity);
		}
	}
	
	private Bundle restoredState;
	private PremiumPurchaseHelper purchaseHelper;
	private PurchaseStatus status;
	private Subscription premiumSubscription;

	@Override
	public CxtView getActionViewName() {
		return CxtView.UPGRADE;
	}
	
	@Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
		return UiEntityIdentifier.PREMIUM_SUBSCRIPTION;
	}
	
	@Override
	public void onViewCreatedImpl(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreatedImpl(view, savedInstanceState);
		restoredState = savedInstanceState;
		
		status = Parceller.get(restoredState, ARG_INFO, PurchaseStatus.JSON_CREATOR);

		if (status == null) {
			fetchInfo();
		}
	}

	private void fetchInfo() {
        showProgress();
		
		// First to need grab the latest account info, so we can know for sure their premium state. This will also send any pending purchase actions that haven't synced yet
		Pocket pocket = pocket();
		pocket.syncRemote(AccountUtil.getuser(pocket().spec()))
				.onSuccess(a -> {
					if (isDetachedOrFinishing()) return;
					if (a.user.premium_alltime_status != PremiumAllTimeStatus.NEVER) {
						// Load premium state
						pocket.syncRemote(pocket.spec().things().purchaseStatus().build())
								.onSuccess(s -> {
									if (isDetachedOrFinishing()) return;
									status = s;
									rebuildPrefs();
								})
								.onFailure(e -> showError(e, v -> fetchInfo()));
					} else {
						// Free users can open directly
						hideProgress();
					}
					
				})
				.onFailure(e -> showError(e, v -> fetchInfo()));
	}

	@Override
	public void onStart() {
		super.onStart();
		premiumSubscription = pocket().subscribe(Changes.of(pocket().spec().things().loginInfo().build()).value(i -> i.account.premium_status),
				i -> {
					if (isDetachedOrFinishing()) return;
					showProgress();
					pocket().syncRemote(pocket().spec().things().purchaseStatus().build())
							.onSuccess(s -> {
								if (isDetachedOrFinishing()) return;
								status = s;
								rebuildPrefs();
								hideProgress();
								if (status != null && status.subscription_info != null && status.subscription_info.is_active) {
									setHeaderVisibility(true);
								} else {
									setHeaderVisibility(false);
								}
							})
							.onFailure(e -> {
								showError(e, v -> fetchInfo());
								setHeaderVisibility(false);
							});
				});
	}
	
	@Override
	protected void createPrefs(ArrayList<Preference> prefs) {
		if (app().pktcache().premium_alltime_status() != PremiumAllTimeStatus.NEVER) {
			// if subscription info is null, don't build prefs yet, we'll get another call to this once subscription info is fetched
			if (status == null || status.subscription_info == null) return;
			PremiumSubscriptionInfo subscriptionInfo = status.subscription_info;
			
			// Subscription Status
			prefs.add(PreferenceViews.newHeader(this, R.string.prem_setting_status));
			
			// Subscription (monthly/yearly)
			prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_subscription)
					.setSummaryDefaultUnchecked(subscriptionInfo.is_active ? subscriptionInfo.subscription_type : getString(R.string.prem_setting_inactive_subscription))
					.build());
			
			// Date purchased
			prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_date_purchased)
					.setSummaryDefaultUnchecked(subscriptionInfo.purchase_date.display())
					.build());
			
			// Renewal date
			prefs.add(PreferenceViews.newActionBuilder(this, subscriptionInfo.is_active ? R.string.prem_setting_renewal_date : R.string.prem_setting_date_ended)
					.setSummaryDefaultUnchecked(subscriptionInfo.renew_date.display())
					.build());
			
			if (subscriptionInfo.is_active) {
				// Purchased From
				if (subscriptionInfo.source_display != null) {
					prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_purchase_location)
							.setSummaryDefaultUnchecked(subscriptionInfo.source_display)
							.build());
				}
			} else {
				prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_renew_subscription)
						.setSummaryDefaultUnchecked(R.string.prem_setting_subscription_ended_tap_to_renew)
						.setOnClickListener(() -> app().premium().showRenewScreen(getActivity(), CxtSource.PREMIUM_SETTINGS))
						.build());
			}
			
			// Your Subscription
			prefs.add(PreferenceViews.newHeader(this, R.string.prem_setting_your_subscription));
			
			// Manage your subscription > google play
			prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_manage_your_subscription)
					.setOnClickListener(() -> {
						if (subscriptionInfo.source == PurchaseSource.GOOGLEPLAY) {
							var url = "https://play.google.com/store/account/subscriptions";
							if (subscriptionInfo.is_active) {
								// If the subscription is active we can append extra details
								// and open subscription details directly.
								url += "?package=com.ideashower.readitlater.pro&sku=" +
										subscriptionInfo.order_id;
							} else {
								// If it's not active, we can only open the main subscriptions
								// screen where the subscription is in the expired section.
							}
							App.viewUrl(getActivity(), url);

						} else if (subscriptionInfo.source == PurchaseSource.WEB) {
							App.viewUrl(getActivity(), "https://getpocket.com/premium/manage");
							
						} else {
							App.viewUrl(getActivity(), "https://help.getpocket.com/customer/portal/articles/1545683");
						}
					})
				.build());
			
			removePurchaseHelper();
			
			if (status.features != null && !status.features.isEmpty()) {
				// Premium features
				prefs.add(PreferenceViews.newHeader(this, R.string.prem_setting_your_premium_features));
				
				List<PremiumFeatureStatus> features = status.features;
				for (final PremiumFeatureStatus feature : features) {
					ActionBuilder action = PreferenceViews.newActionBuilder(this, feature.name)
						.setOnClickListener(() -> App.viewUrl(getActivity(), feature.faq_link.url));
					
					if (feature.status == 0) {
						action.setSummaryDefaultUnchecked(feature.status_text);
					}
					
					prefs.add(action.build());
				}
				
				if (subscriptionInfo.is_active) {
					setHeaderVisibility(true);
				} else {
					setHeaderVisibility(false);
				}
			}
			
		} else {
			// Free
			
			initPurchaseHelper();
			
			// Questions Feedback
			prefs.add(PreferenceViews.newHeader(this, R.string.prem_setting_premium_header));
			
			// Upgrade
			prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_upgrade)
					.setOnClickListener(() -> app().premium().showUpgradeScreen(getActivity(), CxtSource.PREMIUM_SETTINGS))				.build());

			// Restore
			prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_restore)
					.setOnClickListener(() -> purchaseHelper.restorePurchase())
				.build());
			
			setHeaderVisibility(false);
		}
		
		// Questions Feedback
		prefs.add(PreferenceViews.newHeader(this, R.string.prem_setting_questions));
		
		// FAQ
		prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_faq)
				.setOnClickListener(() -> App.viewUrl(getActivity(), "https://help.getpocket.com/customer/portal/articles/1545683"))
			.build());
		
		// Contact Us
		prefs.add(PreferenceViews.newActionBuilder(this, R.string.prem_setting_contact)
				.setOnClickListener(() -> Help.requestHelp(getContext(), Help.getSupportEmail(), "", null, true, false, null, null))
			.build());

        hideProgress();
	}
	
	private void removePurchaseHelper() {
		if (purchaseHelper != null) {
			purchaseHelper.onDestroy();
			purchaseHelper = null;
		}
	}

	private void initPurchaseHelper() {
		if (purchaseHelper == null) {
			purchaseHelper = new PremiumPurchaseHelper(GoogleBillingUtil.PREMIUM, getActivity(), new PurchaseListener() {
				
				@Override
				public void onPurchasingStateChanged(PurchasingState state) {
					if (state == PurchasingState.PURCHASING || state == PurchasingState.ACTIVATING || state == PurchasingState.RESTORING) {
						showProgress();
					} else {
						hideProgress();
					}
				}
				
				@Override
				public void onPremiumPurchased() {
					Toast.makeText(getActivity(), R.string.purchase_restored, Toast.LENGTH_LONG)
						.show();
					app().premium().showPurchaseComplete(getActivity(), CxtSource.PREMIUM_SETTINGS);
				}
				
				// None of the following are expected or needed on this screen
				
				@Override
				public void onGooglePlayUnavailable() {}
				
				@Override
				public void showWebPaymentFlow() {}
				
				@Override
				public void onProductsLoaded(Products products) {}
				
				@Override
				public void onProductsLoadFailed() {}
				
				@Override
				public void onProductPurchaseActivationFailedDialogDismissed() {}
				
				@Override
				public void onPurchaseFailed(boolean isCancel) {}
				
			}, restoredState);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (purchaseHelper != null) {
			purchaseHelper.onSaveInstanceState(outState);
		}
		if (status != null) {
			Parceller.put(outState, ARG_INFO, status);
        }
	}
	
	@Override
	public void onStop() {
		super.onStop();
		premiumSubscription = Subscription.stop(premiumSubscription);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (purchaseHelper != null) {
			purchaseHelper.onDestroy();
		}
	}
	
	@Override
	protected int getTitle() {
		return R.string.mu_premium;
	}

	@Override
	protected View getBannerView() {
		LayoutInflater li = LayoutInflater.from(getActivity());
		return li.inflate(R.layout.view_prefs_header_layout, null);
	}
	
}