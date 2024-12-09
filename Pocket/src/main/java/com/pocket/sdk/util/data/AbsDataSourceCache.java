package com.pocket.sdk.util.data;

import com.ideashower.readitlater.BuildConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A data source cache implementation that handles a lot of the common logic. This can be used as a starting point
 * instead of the raw interface in most cases.
 *
 * All calls are assumed to be made from the UI thread, if not, bugs could occur.
 */
public abstract class AbsDataSourceCache<T> implements DataSourceCache<T> {

    private static final boolean STRICT_MODE = BuildConfig.DEBUG;

    private final List<T> mList = new ArrayList<>();
    private final Set<Listener> mListeners = new HashSet<>();
    private final Thread mCreatedOnThread = STRICT_MODE ? Thread.currentThread() : null;

    private LoadState mLoadState = LoadState.INITIAL;
    private Error mError;
    private boolean mIsPagingComplete;
    private boolean mIsRefreshPending;

    public AbsDataSourceCache() {}

    private void checkThread() {
        if (STRICT_MODE && Thread.currentThread() != mCreatedOnThread) throw new RuntimeException("methods should only be invoked from the ui thread (or the thread the created this cache). This was created on thread: " + mCreatedOnThread);
    }

    @Override
    public void loadFirstPage() {
        checkThread();
        // Check if already loaded and can ignore:
        switch (mLoadState) {
            case INITIAL_LOADING:
            case LOADED_APPENDING:
            case LOADED_APPEND_ERROR:
            case LOADED_REFRESH_ERROR:
            case LOADED_REFRESHING:
                return; // Already loaded or in process of loading

            case LOADED:
                if (size() > 0) {
                    return; // Already loaded
                }
                // If loaded and empty, ok to retry to see if there is now new data
                break;

            // All other states are ok to try loading
        }

        setState(LoadState.INITIAL_LOADING);
        doLoadFirstPage();
    }

    /**
     * // must setList if successful, or set mError and setState(iniital_error)
     */
    protected abstract void doLoadFirstPage();

    @Override
    public void loadNextPage() {
        checkThread();
        switch (mLoadState) {
            case INITIAL:
            case INITIAL_ERROR:
                // Load first page instead
                loadFirstPage();
                return;

            case INITIAL_LOADING:
            case LOADED_APPENDING:
                // Already loading, do nothing
                return;

            case LOADED_REFRESHING:
                // Attempting a refresh, do not load more pages
                return;
        }

        if (isPagingComplete()) {
            // No more pages to load
            return;
        }

        // Append
        setState(LoadState.LOADED_APPENDING);
        doLoadNextPage();
    }

    /**
     * // copy list, append and setlist, or handle error
     */
    protected abstract void doLoadNextPage();

    @Override
    public void refresh() {
        checkThread();
        switch (mLoadState) {
            case INITIAL:
            case INITIAL_ERROR:
            case INITIAL_LOADING:
                // Load first page instead
                loadFirstPage();
                return;

            case LOADED_REFRESHING:
                // Already refreshing, mark that we should refresh again afterwards
                // This makes sure if something changed that required a refresh while it was refreshing, we go get the latest data again later.
                mIsRefreshPending = true;
                return;
        }

        setState(LoadState.LOADED_REFRESHING);
        doRefresh();
    }

    /**
     * // setList, if pending refresh, refresh, handle error can cancel pending refresh
     */
    protected abstract void doRefresh();

    @Override
    public int size() {
        checkThread();
        return mList.size();
    }

    @Override
    public T get(int position) {
        checkThread();
        return mList.get(position);
    }
    
    public int indexOf(T item) {
        checkThread();
        return mList.indexOf(item);
    }

    @Override
    public LoadState getState() {
        checkThread();
        return mLoadState;
    }

    @Override
    public Error getError() {
        checkThread();
        return mError;
    }

    @Override
    public void addListener(Listener listener) {
        checkThread();
        mListeners.add(listener);
    }

    public void clearListeners() {
        mListeners.clear();
    }

    protected void setState(LoadState state) {
        checkThread();
        if (state == mLoadState) {
            return;
        }

        mLoadState = state;
        for (Listener listener : new ArrayList<>(mListeners)) {
            listener.onDataSourceStateChanged(state);
        }
    }

    protected void setError(Error error, LoadState state) {
        checkThread();
        mError = error;
        setState(state);
    }

    @Override
    public boolean isPagingComplete() {
        checkThread();
        return mIsPagingComplete;
    }

    @Override public List<T> getList() {
        checkThread();
        return mList;
    }
    
    protected void setList(List<T> list, boolean isPagingComplete) {
        checkThread();
        mList.clear();
        mList.addAll(list);
        mIsPagingComplete = isPagingComplete;
        setState(LoadState.LOADED);
        for (Listener listener : mListeners) {
            listener.onDataSourceChanged();
        }
    }

    /**
     * @return true if this is in one of {@link com.pocket.sdk.util.data.DataSourceCache.LoadState}'s LOADED_ (has content) states.
     */
    public boolean isLoaded() {
        checkThread();
        switch (getState()) {
            case LOADED:
            case LOADED_APPEND_ERROR:
            case LOADED_REFRESHING:
            case LOADED_REFRESH_ERROR:
                return true;
            default:
                return false;
        }
    }
}
