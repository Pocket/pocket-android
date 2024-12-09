package com.pocket.app.list.list

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.enums.ItemSortKey
import com.pocket.sdk.api.generated.enums.ItemStatusKey
import com.pocket.sdk.api.generated.enums.ReservedTag
import com.pocket.sdk.api.generated.thing.Get
import com.pocket.sdk.api.generated.thing.Saves
import com.pocket.sdk.api.thing.GetUtil
import com.pocket.sdk.util.data.DataSourceCache
import com.pocket.sdk.util.data.SyncCache
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.util.NoCompareMutableStateFlow
import com.pocket.util.NoCompareStateFlow
import com.pocket.util.equalsAny
import com.pocket.util.prefs.Preferences
import com.pocket.util.prefs.StringPreference
import com.pocket.util.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListManager @Inject constructor(
    private val pocket: Pocket,
    prefs: Preferences,
    private val pocketCache: PocketCache,
) {

    private val sortPreference: StringPreference = prefs.forUser("sort", ItemSortKey.NEWEST.value)

    private val _sortFilterState = MutableStateFlow(
        SortFilterState(
            sort = ItemSortKey.find(sortPreference.get()) ?: ItemSortKey.NEWEST,
        )
    )
    val sortFilterState: StateFlow<SortFilterState> = _sortFilterState
    private val _list = NoCompareMutableStateFlow<List<Any>>(listOf())
    val list: NoCompareStateFlow<List<Any>> = _list
    private val _loadState = MutableStateFlow(DataSourceCache.LoadState.INITIAL)
    val loadState: StateFlow<DataSourceCache.LoadState> = _loadState

    var isSearching: Boolean = false
    var isRemoteData: Boolean = false

    private var syncCache: SyncCache<Any, *>? = null
        set(value) {
            field?.clearListeners()
            value?.let { field = it } ?: return
            value.addListener(object : DataSourceCache.Listener {
                override fun onDataSourceChanged() {
                    _list.update { value.list }
                }

                override fun onDataSourceStateChanged(state: DataSourceCache.LoadState?) {
                    // fallback to offline my list search if there is an error
                    // when we are doing a remote search for my list
                    if (state.equalsAny(
                            DataSourceCache.LoadState.INITIAL_ERROR,
                            DataSourceCache.LoadState.LOADED_APPEND_ERROR,
                            DataSourceCache.LoadState.LOADED_REFRESH_ERROR
                        )
                        && sortFilterState.value.listStatus == ListStatus.SAVES
                        && isSearching
                        && syncCache?.identity() is Get
                    ) {
                        loadMyList()
                        return
                    }
                    _loadState.update { value.state }
                }
            })
        }

    init {
        refreshCache()
    }

    private fun refreshCache() {
        if (sortFilterState.value.listStatus == ListStatus.SAVES) {
            if (isSearching && pocketCache.hasPremium()) {
                loadRemote(ItemStatusKey.UNREAD, ItemSortKey.RELEVANCE)
            } else {
                loadMyList()
            }
        } else {
            if (isSearching  && pocketCache.hasPremium()) {
                loadRemote(ItemStatusKey.ARCHIVE, ItemSortKey.RELEVANCE)
            } else {
                loadRemote(ItemStatusKey.ARCHIVE)
            }
        }
    }

    private fun loadMyList() {
        syncCache = SyncCache.from(pocket)
            .sync(pocket.spec().things().saves()
                .state(ItemStatusKey.UNREAD)
                .search(sortFilterState.value.search)
                .sort(sortFilterState.value.sort)
                .filters(sortFilterState.value.filters)
                .tag(sortFilterState.value.tag)
                .build()
            )
            .display { saves ->
                mutableListOf<Any>().apply {
                    saves.list?.let { savesList ->
                        addAll(savesList)
                    }
                }
            }
            .pageByPosition { t: Saves, subset: SyncCache.Subset ->
                t.builder()
                    .offset(subset.offset)
                    .count(subset.count)
                    .build()
            }
            .build()
        isRemoteData = false
        syncCache?.loadFirstPage()
    }

    /**
     * Archives are not saved to device so we need to use a different sync cache that
     * gets the remote items
     */
    private fun loadRemote(
        itemStatusKey: ItemStatusKey,
        itemSortKey: ItemSortKey = sortFilterState.value.sort
    ) {
        syncCache = SyncCache.from(pocket)
            .sync(GetUtil.setAllItemFlags(pocket.spec().things().get())
                .state(itemStatusKey)
                .search(sortFilterState.value.search)
                .sort(
                    when (itemSortKey) {
                        ItemSortKey.SHORTEST, ItemSortKey.LONGEST -> {
                            // Not available in v3 API, use a fallback.
                            ItemSortKey.NEWEST
                        }
                        else -> itemSortKey
                    }
                )
                .apply {
                    val filters = sortFilterState.value.filters
                    if (filters.contains(ItemFilterKey.FAVORITE)) favorite(true)
                    if (filters.contains(ItemFilterKey.HIGHLIGHTED)) hasAnnotations(true)
                    if (filters.contains(ItemFilterKey.NOT_TAGGED)) tag(ReservedTag._UNTAGGED_.value)
                }
                .tag(sortFilterState.value.tag)
                .build())
            .display { get: Get? ->
                mutableListOf<Any>().apply {
                    addAll(GetUtil.list(get))
                }
            }
            .pageByPosition { get: Get, subset: SyncCache.Subset ->
                get.builder()
                    .count(subset.count)
                    .offset(subset.offset)
                    .build()
            }
            .build()
        syncCache?.forceRemote = true
        isRemoteData = true
        syncCache?.loadFirstPage()
    }

    fun loadNextPage() {
        syncCache?.loadNextPage()
    }

    fun setTag(tag: String?) {
        _sortFilterState.update { it.copy(
            tag = tag,
            filters = listOf(ItemFilterKey.TAG)
        ) }
        refreshCache()
    }

    fun updateCurrentSort(itemSortKey: ItemSortKey) {
        sortPreference.set((itemSortKey).value)
        _sortFilterState.update { it.copy(
            sort = itemSortKey
        ) }
        refreshCache()
    }

    fun onFilterToggled(filterKey: ItemFilterKey) {
        _sortFilterState.update { it.copy(
            tag = null,
            filters = if (sortFilterState.value.filters.contains(filterKey)) {
                listOf()
            } else {
                listOf(filterKey)
            }
        ) }
        refreshCache()
    }

    fun addFilter(filterKey: ItemFilterKey) {
        _sortFilterState.update { it.copy(
            tag = null,
            filters = listOf(filterKey)
        ) }
        refreshCache()
    }

    fun clearFilters() {
        _sortFilterState.update { it.copy(
            tag = null,
            filters = listOf()
        ) }
        refreshCache()
    }

    fun setStatusFilter(statusFilter: ListStatus) {
        _sortFilterState.update { it.copy(
            tag = null,
            filters = listOf(),
            listStatus = statusFilter
        ) }
        refreshCache()
    }

    fun setSearchText(text: String) {
        _sortFilterState.update { it.copy(
            search = text
        ) }
        refreshCache()
    }
}

data class SortFilterState(
    val sort: ItemSortKey = ItemSortKey.NEWEST,
    val filters: List<ItemFilterKey> = mutableListOf(),
    val listStatus: ListStatus = ListStatus.SAVES,
    val tag: String? = null,
    val search: String = "",
)

enum class ListStatus {
    SAVES,
    ARCHIVE
}
