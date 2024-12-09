package com.pocket.sdk.util.data;

import java.util.List;

/**
 * Holds cached/loaded data of a certain type.
 */
public interface DataSourceCache<T> {

    /**
     * The state of the data.
     */
    enum LoadState {
        /**
         * Initial state. {@link #loadFirstPage()} hasn't been invoked yet.
         */
        INITIAL,

        /**
         * {@link #loadFirstPage()} has been invoked and the data is trying to load.
         */
        INITIAL_LOADING,

        /**
         * {@link #loadFirstPage()} failed.
         * @see #getError() for more details.
         */
        INITIAL_ERROR,

        /**
         * The data has been loaded. This is the success result of any of the data changing methods
         * including {@link #loadFirstPage()}, {@link #loadNextPage()} and {@link #refresh()}.
         *
         * @see #size() To check if empty.
         * @see #isPagingComplete() To check if there are more pages to load
         */
        LOADED,

        /**
         * We have at least the first page. We are trying to load the next page.
         * @see #loadNextPage()
         */
        LOADED_APPENDING,

        /**
         * We have at least the first page. We failed to the load the next page.
         * @see #getError() for more details.
         * @see #loadNextPage()
         */
        LOADED_APPEND_ERROR,
        /**
         * Same as {@link #LOADED}, but {@link #refresh()} was invoked
         * and the data may be updated in the near future.
         */
        LOADED_REFRESHING,
        /**
         * Same as {@link #LOADED}, but a recent attempt to {@link #refresh()} failed.
         * @see #getError() for more details.
         */
        LOADED_REFRESH_ERROR,
    }

    /**
     * Asynchronously load the first page of the data. If the data already has the first page,
     * this method as no effect.
     * <p>
     * Listener.onStateChanged will be invoked as the state changes from loading, to loaded or to an error.
     * <p>
     * {@link LoadState#INITIAL_LOADING} will be immediately after this.
     * {@link LoadState#INITIAL_ERROR} will be in the future on failure.
     * {@link LoadState#LOADED} will be in the future on success. Along with {@link Listener#onDataSourceChanged()}.
     */
    void loadFirstPage();

    /**
     * Asynchronously load the next page of the data. Only one additional page will be
     * fetched at a time, so if multiple calls to this method are invoked before the next page
     * loads, they will have no effect. Once the page loads, this method will once again load the next
     * page when invoked. Also, if {@link LoadState#isPagingComplete()} this will do nothing
     * and no callbacks will be invoked. If this is refreshing, this will also do nothing.
     * <p>
     * Listener.onStateChanged will be invoked as the state changes from loading, to loaded or to an error.
     * <p>
     * {@link LoadState#LOADED_APPENDING} will be immediately after this.
     * {@link LoadState#LOADED_APPEND_ERROR} will be in the future on failure.
     * {@link LoadState#LOADED} will be in the future on success. Along with {@link Listener#onDataSourceChanged()}.
     *
     */
    void loadNextPage();

    /**
     * Asynchronously check if the data is out of date and if so, replace the current data with
     * the latest ones. This will reset the data's size back to only the first page.
     * If an error occurs, it will keep the current data intact. If the data hasn't loaded
     * its first page yet, this will trigger {@link #loadFirstPage()} instead.
     * <p>
     * Listener.onStateChanged will be invoked as the state changes from loading, to loaded or to an error.
     * <p>
     * {@link LoadState#LOADED_REFRESHING} will be immediately after this.
     * {@link LoadState#LOADED_REFRESH_ERROR} will be in the future on failure.
     * {@link LoadState#LOADED} will be in the future on success. Along with {@link Listener#onDataSourceChanged()}.
     *
     */
    void refresh();

    /**
     * @return The current size of the loaded data. This will change over time as additional pages
     *          are loaded or as the data refreshes.
     */
    int size();

    boolean isPagingComplete();

    /**
     * Returns the item at a specific position in the cached list.
     * @param position A position from 0 to size()-1. Throws exception if out of range.
     * @return The item, never null.
     */
    T get(int position);

    /**
     * @return All items in the list
     */
    List<T> getList();

    /**
     * @return The current LoadState. Listener.onStateChanged() will keep you updated on this if you use the listener.
     */
    LoadState getState();

    /**
     * @return The last error or null
     *
     * @see LoadState#INITIAL_ERROR
     * @see LoadState#LOADED_APPEND_ERROR
     * @see LoadState#LOADED_REFRESH_ERROR
     */
    Error getError();

    /**
     * Listen to data set changes and loading state changes.
     */
    void addListener(Listener listener);

    interface Listener {
        /**
         * Invoked anytime the data's items change.
         */
        void onDataSourceChanged();

        /**
         * Invoked as the load state changes.
         */
        void onDataSourceStateChanged(LoadState state);
    }

    interface Error {
        /**
         * Retry whatever triggered this error.
         */
        void retry();

        
        /** @return The throwable if there is one involved. Provide a {@link com.pocket.util.java.UserFacingErrorMessage} if possible. */
        Throwable getError();
    }
}
