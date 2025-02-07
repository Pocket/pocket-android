package com.pocket.sdk.premium.billing;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.help.Help;
import com.pocket.sdk.premium.billing.PremiumBillingError.Type;
import com.pocket.sdk.premium.billing.google.GooglePlayBilling;
import com.pocket.sdk.premium.billing.google.GooglePlayProduct;
import com.pocket.sdk.premium.billing.google.ProductList;
import com.pocket.sdk.premium.billing.google.Products;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.sdk.util.dialog.AlertMessaging;

/**
 * Helper class for user facing UI when connecting with {@link GooglePlayBilling}.
 * <p>
 * Handles a lot of the common errors and cases automatically. Displaying messages or invoking callbacks as needed. See {@link GooglePlayBilling}
 * for more info. Most of the methods are the same names.
 * <p>
 * Be sure to invoke all of the following methods for full function.
 * <ul>
 * <li>Invoke {@link #onDestroy()} when you are done with this.</li>
 * <li>Invoke {@link #onSaveInstanceState(Bundle)} from your fragment or activity's method of the same name.</li>
 * </ul>
 */
public class PremiumPurchaseHelper implements PremiumBillingCallbacks {
	
	private static final String HELP_URL_ACTIVATION = "https://support.mozilla.org/kb/trouble-activating-or-restoring-your-pocket-premium-subscription";
	
	public enum PurchasingState {
		PURCHASING, RESTORING, ACTIVATING, IDLE
	}

	private final GooglePlayBilling mBilling;
	private final Activity mActivity;
	private final PurchaseListener mPurchaseListener;
	
	private boolean mHasManuallyRequestedProducts;
	private boolean mIsGooglePlayBillingUnavailable;
	private Products mProducts;
	private PurchasingState mState;
	
	public PremiumPurchaseHelper(ProductList skus, Activity activity, PurchaseListener listener, Bundle savedInstanceState) {
		mActivity = activity;
		mPurchaseListener = listener;
		
		mBilling = new GooglePlayBilling(skus, activity, this, savedInstanceState);
		mBilling.getProductsAsync();
	}
	
	public void onSaveInstanceState(Bundle outState) {
		mBilling.onSaveInstanceState(outState);
	}
	
	/**
	 * Load the products. Will receive them via {@link #onProductsLoaded(Products)} or an error will be shown to the user.
	 */
	public void getProductsAsync() {
		mHasManuallyRequestedProducts = true;
		
		if (mIsGooglePlayBillingUnavailable) {
			mPurchaseListener.onGooglePlayUnavailable();
			
		} else if (!App.from(mActivity).http().status().isOnline()) {
			showOfflineError();
			mPurchaseListener.onProductsLoadFailed();
			
		} else {
			mBilling.getProductsAsync();
		}
	}
	
	/**
	 * Start the purchase process for a product.
	 * @param product
	 */
	public void startPurchase(PremiumProduct product) {
		App app = App.from(mActivity);
		if (app.pktcache().hasPremiumAndPaid()) {
			showAlreadySubscribedError();
			mPurchaseListener.onPurchasingStateChanged(PurchasingState.IDLE);
			mPurchaseListener.onPurchaseFailed(false);
			
		} else if (!app.http().status().isOnline()) {
			showOfflineError();
			mPurchaseListener.onPurchasingStateChanged(PurchasingState.IDLE);
			mPurchaseListener.onPurchaseFailed(false);
			
		} else {
			setPurchasingState(PurchasingState.PURCHASING);
			mBilling.startPurchase(product);
		}
	}
	
	/**
	 * Search for a subscription purchase in Google Play and if found activate it, or display an error.
	 */
	public void restorePurchase() {
		App app = App.from(mActivity);
		if (app.pktcache().hasPremiumAndPaid()) {
			onProductPurchaseSuccess();
			
		} else if (!app.http().status().isOnline()) {
			setPurchasingState(PurchasingState.IDLE);
			showOfflineError();
			mPurchaseListener.onPurchaseFailed(false);
			
		} else if (mProducts != null) {
			if (restore(mProducts.getYearly())) {
				// Activation started
			} else if (restore(mProducts.getMonthly())) {
				// Activation started
			} else {
				setPurchasingState(PurchasingState.IDLE);
				showNoSubscriptionsFoundError();
				mPurchaseListener.onPurchaseFailed(false);
			}
			
		} else {
			setPurchasingState(PurchasingState.RESTORING);
			mBilling.getProductsAsync();
		}
	}
	
	private boolean restore(GooglePlayProduct product) {
		if (product != null && product.isPurchased()) {
			mBilling.activatePurchase(product, product.getPurchaseData(), GooglePlayBilling.SendType.RESTORE);
			return true;
		}
		return false;
	}
	
	private void showAlreadySubscribedError() {
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.purchase_error_already_owned_t)
			.setMessage(mActivity.getString(R.string.purchase_error_already_owned_m))
			.setPositiveButton(R.string.ac_ok, null)
			.setNegativeButton(R.string.ac_get_help, (dialog, which) -> Help.requestHelp(mActivity,
					Help.getSupportEmail(),
					"Android: Already Subscribed",
					null, true, false, null, null))
			.show();
	}	
	
	private void showNoSubscriptionsFoundError() {
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.purchase_error_restore_none_found_t)
			.setMessage(mActivity.getString(R.string.purchase_error_restore_none_found_m, mActivity.getString(R.string.ac_get_help)))
			.setPositiveButton(R.string.ac_ok, null)
			.setNegativeButton(R.string.ac_get_help, (dialog, which) -> Help.requestHelp(mActivity,
					Help.getSupportEmail(),
					"Android: No Subscription Found",
					null, true, false, null, null))
			.show();
	}	

	@Override
	public void onProductsLoaded(Products products) {
		mProducts = products;
		mPurchaseListener.onProductsLoaded(products);
		if (mState == PurchasingState.RESTORING) {
			restorePurchase();
		}
	}
	
	@Override
	public void onProductsLoadError(PremiumBillingError error) {
		if (mState == PurchasingState.RESTORING) {
			setPurchasingState(PurchasingState.IDLE);
			showNoSubscriptionsFoundError();
			
		} else {
			setPurchasingState(PurchasingState.IDLE);
			
			if (!mHasManuallyRequestedProducts) {
				/*
				 * The user hasn't clicked the butotn yet. This is just the automatic load on page open that failed.
				 * Ignore for now and let them click the button to retry. 
				 * 
				 * However if it was a fatal error, take note so we don't bother retrying later.
				 */
				mIsGooglePlayBillingUnavailable = error.getType() == Type.FATAL;
				
			} else {
				// This error occurred after a user requested the load through a button tap, need to provide some feedback
				switch (error.getType()) {
				case FATAL:
					mPurchaseListener.onGooglePlayUnavailable();
					break;
				case TEMPORARY:
					showOfflineError();
					break;
				case CANCEL:
					// Ignore
					break;
				default:
					// Not expected to have others.
					break;
				}
			}
		}
		
		mPurchaseListener.onProductsLoadFailed();
	}
	
	private void showOfflineError() {
		if (AlertMessaging.isContextUnavailable(mActivity)) return; // Ignore if we can't display an error
		new AlertDialog.Builder(mActivity)
			.setTitle(R.string.purchase_error_offline_t)
			.setMessage(R.string.purchase_error_offline_m)
			.setPositiveButton(R.string.ac_ok, null)
			.setNegativeButton(R.string.ac_get_help, (dialog, which) -> Help.requestHelp(mActivity,
					Help.getSupportEmail(),
					"Android: Purchase Not Successful",
					null, true, false, null, null))
			.show();
	}
	
	private void setPurchasingState(PurchasingState state) {
		if (mState == state) {
			return;
		}
		mState = state;
		mPurchaseListener.onPurchasingStateChanged(state);
	}
	
	@Override
	public void onProductPurchaseFailed(final PremiumBillingError error) {
		setPurchasingState(PurchasingState.IDLE);
		
		switch (error.getType()) {
		case ALREADY_PURCHASED:
			if (App.getApp().pktcache().hasPremiumAndPaid()) {
				showAlreadySubscribedError();
			} else {
				// They already have purchased this, restore the purchase
				restorePurchase();
			}
			mPurchaseListener.onPurchaseFailed(false);
			break;
			
		case CANCEL:
			// Nothing to do.
			mPurchaseListener.onPurchaseFailed(true);
			break;
			
		default:	
			new AlertDialog.Builder(mActivity)
				.setTitle(R.string.purchase_error_purchase_t)
				.setMessage(R.string.purchase_error_purchase_m)
				.setPositiveButton(R.string.ac_ok, null)
				.setNegativeButton(R.string.ac_get_help, (dialog, which) -> Help.requestHelp(mActivity,
						Help.getSupportEmail(),
						"Android: Purchase Not Successful",
						null, true, false, error.getReportableError(), null))
				.show();
			mPurchaseListener.onPurchaseFailed(false);
		}
	}
	
	@Override
	public void onProductPurchaseActivationFailed(ErrorReport error) {
		setPurchasingState(PurchasingState.IDLE); 
		
		AlertDialog dialog = new AlertDialog.Builder(mActivity)
			.setTitle(R.string.purchase_error_activation_t)
			.setMessage(R.string.purchase_error_activation_m)
			.setPositiveButton(R.string.ac_ok,
					(dialog12, which) -> App.viewUrl(mActivity, HELP_URL_ACTIVATION))
			.create();
		
		dialog.setOnDismissListener(dialog1 ->
				mPurchaseListener.onProductPurchaseActivationFailedDialogDismissed()
		);
		
		dialog.show();
	}
	
	@Override
	public void onProductPurchaseActivationStarted() {
		setPurchasingState(PurchasingState.ACTIVATING);
	}
	
	@Override
	public void onProductPurchaseSuccess() {
		setPurchasingState(PurchasingState.IDLE);
		mPurchaseListener.onPremiumPurchased();
	}
	
	public void onDestroy() {
		if (mBilling != null) {
			mBilling.disconnect();
		}
	}

	public interface PurchaseListener {
		
		void onProductsLoaded(Products products);
		void onProductsLoadFailed();
		void onGooglePlayUnavailable();
		void showWebPaymentFlow();
		
		/** 
		 * A purchase attempt has begun or stopped.
		 */
		void onPurchasingStateChanged(PurchasingState state);
		
		/**
		 * The purchase failed.
		 * @param isCancel true if user canceled, false if error.
		 */
		void onPurchaseFailed(boolean isCancel);
		
		/**
		 * A purchase was successful.
		 */
		void onPremiumPurchased();
		
		/**
		 * The fragment/screen should be closed.
		 */
		void onProductPurchaseActivationFailedDialogDismissed();
	}
	
}
