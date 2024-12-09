package com.pocket.app.list

import android.view.View
import app.cash.turbine.test
import com.ideashower.readitlater.R
import com.pocket.BaseCoroutineTest
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.SavesEvents
import com.pocket.analytics.appevents.SavesTab
import com.pocket.app.list.list.ListManager
import com.pocket.app.list.list.ListStatus
import com.pocket.app.list.list.SortFilterState
import com.pocket.app.undobar.UndoBar
import com.pocket.app.undobar.UndoableItemAction
import com.pocket.data.models.toDomainItem
import com.pocket.fakes.fakeItem
import com.pocket.repository.ItemRepository
import com.pocket.repository.SearchRepository
import com.pocket.repository.TagRepository
import com.pocket.sdk.api.AppSync
import com.pocket.sdk.api.generated.enums.ItemFilterKey
import com.pocket.sdk.api.generated.enums.ItemSortKey
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.Tags
import com.pocket.sdk.api.value.HtmlString
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk.offline.OfflineDownloading
import com.pocket.sdk.util.data.DataSourceCache
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.sdk2.view.ModelBindingHelper
import com.pocket.testutils.SharedFlowTracker
import com.pocket.util.NoCompareMutableStateFlow
import com.pocket.util.StringLoader
import com.pocket.util.edit
import com.pocket.util.update
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MyListViewModelTest : BaseCoroutineTest() {

    @SpyK
    private val listManager = mockk<ListManager>(relaxed = true)
    @SpyK
    private val itemRepository = mockk<ItemRepository>(relaxed = true)
    @SpyK
    private val undoable = mockk<UndoBar>(relaxed = true)
    @SpyK
    private val modelBindingHelper = mockk<ModelBindingHelper>(relaxed = true)
    @SpyK
    private val stringLoader = mockk<StringLoader>(relaxed = true)
    @SpyK
    private val tagRepository = mockk<TagRepository>(relaxed = true)
    @SpyK
    private val pocketCache = mockk<PocketCache>(relaxed = true)
    @SpyK
    private val offlineDownloading = mockk<OfflineDownloading>(relaxed = true)
    @SpyK
    private val appSync = mockk<AppSync>(relaxed = true)
    @SpyK
    private val searchRepository = mockk<SearchRepository>(relaxed = true)
    @SpyK
    private val tracker = mockk<Tracker>(relaxed = true)
    @SpyK
    private val contentOpenTracker = mockk<ContentOpenTracker>(relaxed = true)

    private lateinit var subject: MyListViewModel

    private val state: MyListUiState
        get() = subject.uiState.value

    private val list: List<ListItemUiState>
        get() = subject.listState.value

    private val listManagerLoadStateFlow = MutableStateFlow(
        DataSourceCache.LoadState.LOADED
    )
    private lateinit var navigationEventTracker: SharedFlowTracker<MyListNavigationEvent>

    private val listManagerSortFilterState = MutableStateFlow(
        SortFilterState(
            sort = ItemSortKey.NEWEST,
            filters = listOf(),
            listStatus = ListStatus.SAVES,
            tag = null
        )
    )

    private val testItem1 = fakeItem(
        title = "title",
        domain = "domain",
        topImageUrl = "example.com",
        favorite = true, viewed = true
    )

    private val testItem2 = fakeItem(title = "title2", domain = "domain2")

    private val listManagerList = NoCompareMutableStateFlow<List<Any>>(
        listOf(
            testItem1,
            testItem2
        )
    )

    private val tagFlow = MutableSharedFlow<Tags>(extraBufferCapacity = 1)

    @BeforeTest
    fun setup() {
        every { listManager.list } returns listManagerList
        every { listManager.loadState } returns listManagerLoadStateFlow
        every { listManager.sortFilterState } returns listManagerSortFilterState
        every { stringLoader.getString(R.string.ac_select_all) } returns "select all"
        val quantity = slot<Int>()
        every {
            stringLoader.getQuantityString(
                R.plurals.lb_selected,
                capture(quantity),
                any()
            )
        } answers { "${quantity.captured} selected" }
        every { tagRepository.getTagsAsFlow() } returns tagFlow
        every { pocketCache.isLoggedIn } returns true
        subject = MyListViewModel(
            listManager,
            itemRepository,
            undoable,
            modelBindingHelper,
            stringLoader,
            tagRepository,
            pocketCache,
            offlineDownloading,
            appSync,
            searchRepository,
            tracker,
            contentOpenTracker,
        )
        navigationEventTracker = SharedFlowTracker(subject.navigationEvents)
    }

    @Test
    fun `WHEN onScrolledNearBottom is called THEN load the next page in the list manager`() {
        subject.onScrolledNearBottom()
        verify {
            listManager.loadNextPage()
        }
    }

    @Test
    fun `GIVEN my list chip is not selected WHEN the chip is clicked THEN we filter by saves`() {
        listManagerSortFilterState.edit {
            copy(
                listStatus = ListStatus.ARCHIVE
            )
        }
        subject.onMyListChipClicked()
        // once for onInitialized
        verify(exactly = 1) {
            listManager.setStatusFilter(ListStatus.SAVES)
        }
    }

    @Test
    fun `GIVEN archive chip is selected WHEN this chip is clicked THEN nothing happens`() {
        listManagerSortFilterState.edit {
            copy(
                listStatus = ListStatus.ARCHIVE
            )
        }
        subject.onArchiveChipClicked()
        verify(exactly = 0) {
            listManager.setStatusFilter(ListStatus.ARCHIVE)
        }
    }

    @Test
    fun `GIVEN archive chip is not selected WHEN this chip is clicked THEN we filter by archive`() {
        subject.onArchiveChipClicked()
        verify(exactly = 1) {
            listManager.setStatusFilter(ListStatus.ARCHIVE)
        }
    }

    @Test
    fun `All chip behavior`() {
        // All is already selected so nothing happens when clicking it
        subject.onAllChipClicked()
        verify(exactly = 0) {
            listManager.clearFilters()
        }

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.FAVORITE)
            )
        }

        // All is not selected, so clear filters in list manager
        subject.onAllChipClicked()
        verify(exactly = 1) {
            listManager.clearFilters()
        }
    }

    @Test
    fun `Tagged chip behavior`() {
        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf())
                .build()
        )

        // tagged is unselected and there are no tags
        subject.onTaggedChipClicked()
        verify { listManager.addFilter(ItemFilterKey.TAG) }
        assertEquals(
            actual = subject.navigationEvents.replayCache.size,
            expected = 0
        )

        // tagged is selected and there are no tags
        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.TAG)
            )
        }
        subject.onTaggedChipClicked()
        verify(exactly = 1) {
            listManager.clearFilters()
        }

        tagFlow.tryEmit(
            Tags.Builder()
                .tags(listOf("test"))
                .build()
        )

        // tagged is selected and there are tags
        subject.onTaggedChipClicked()
        verify(exactly = 1) {
            listManager.clearFilters()
        }
        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = MyListNavigationEvent.ShowTagBottomSheet
        )
    }

    @Test
    fun `Favorites chip behavior`() {
        // favorites already selected, don't add filter
        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.FAVORITE)
            )
        }
        subject.onFavoritesChipClicked()
        verify(exactly = 0) {
            listManager.addFilter(ItemFilterKey.FAVORITE)
        }

        // favorites not select, add the filter
        listManagerSortFilterState.edit {
            copy(
                filters = listOf()
            )
        }
        subject.onFavoritesChipClicked()
        verify(exactly = 1) {
            listManager.addFilter(ItemFilterKey.FAVORITE)
        }

        // favorites is selected, click again to clear filters
        subject.onFavoritesChipClicked()
        verify(exactly = 1) {
            listManager.clearFilters()
        }
    }

    @Test
    fun `Highlights chip behavior`() {
        // highlights already selected, don't add filter
        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.HIGHLIGHTED)
            )
        }
        subject.onHighlightsChipClicked()
        verify(exactly = 0) {
            listManager.addFilter(ItemFilterKey.HIGHLIGHTED)
        }

        // highlights not select, add the filter
        listManagerSortFilterState.edit {
            copy(
                filters = listOf()
            )
        }
        subject.onHighlightsChipClicked()
        verify(exactly = 1) {
            listManager.addFilter(ItemFilterKey.HIGHLIGHTED)
        }

        // highlights is selected, click again to clear filters
        subject.onHighlightsChipClicked()
        verify(exactly = 1) {
            listManager.clearFilters()
        }
    }

    @Test
    fun `Bulk Edit setup`() {
        subject.onEditChipClicked()
        assertEquals(state.editChipState, ChipState(selected = true))
        assertEquals(state.bulkEditSnackBarText, "select all")
        assertEquals(state.bulkEditTextClickable, true)
        assertEquals(state.bulkEditActionsEnabled, false)
        assertEquals(list.first().isSelectedForBulkEdit, false)

        subject.onItemSelectedForBulkEdit(testItem1)
        assertEquals(state.bulkEditSnackBarText, "1 selected")
        assertEquals(state.bulkEditTextClickable, false)
        assertEquals(state.bulkEditActionsEnabled, true)
        assertEquals(list.first().isSelectedForBulkEdit, true)
    }

    @Test
    fun `Bulk Edit Re add`() {
        // list is cleared after calling subject.onBulkReAddClicked so lets capture the
        // items in the list so we can make assertions
        val itemsForBulkEditCapture = slot<List<Item>>()
        val itemsForBulkEdit = mutableListOf<Item>()
        every { itemRepository.unArchive(capture(itemsForBulkEditCapture)) } answers {
            itemsForBulkEdit.addAll(itemsForBulkEditCapture.captured)
        }
        subject.onEditChipClicked()
        subject.onItemSelectedForBulkEdit(testItem1)
        subject.onBulkReAddClicked()

        verify {
            itemRepository.unArchive(allAny() as List<Item>)
        }
        assertEquals(1, itemsForBulkEdit.size)

        assertBulkEditCleared()
    }

    @Test
    fun `Bulk edit archive`() {
        // list is cleared after calling subject.onBulkArchiveClicked so lets capture the
        // items in the list so we can make assertions
        val itemsForBulkEditCapture = slot<List<UndoableItemAction>>()
        val itemsForBulkEdit = mutableListOf<UndoableItemAction>()
        every { undoable.archive(capture(itemsForBulkEditCapture)) } answers {
            itemsForBulkEdit.addAll(itemsForBulkEditCapture.captured)
        }
        subject.onEditChipClicked()
        subject.onItemSelectedForBulkEdit(testItem1)
        subject.onBulkArchiveClicked()

        verify {
            undoable.archive(allAny() as List<UndoableItemAction>)
        }
        assertEquals(1, itemsForBulkEdit.size)

        assertBulkEditCleared()
    }

    @Test
    fun `Bulk edit delete`() {
        // list is cleared after calling subject.onBulkDeleteClicked so lets capture the
        // items in the list so we can make assertions
        val itemsForBulkEditCapture = slot<List<Item>>()
        val itemsForBulkEdit = mutableListOf<Item>()
        every { undoable.delete(capture(itemsForBulkEditCapture)) } answers {
            itemsForBulkEdit.addAll(itemsForBulkEditCapture.captured)
        }
        subject.onEditChipClicked()
        subject.onItemSelectedForBulkEdit(testItem1)
        subject.onBulkDeleteClicked()

        verify {
            undoable.delete(allAny() as List<Item>)
        }
        assertEquals(1, itemsForBulkEdit.size)

        assertBulkEditCleared()
    }

    @Test
    fun `Bulk edit overflow`() {
        subject.onEditChipClicked()
        subject.onItemSelectedForBulkEdit(testItem1)
        subject.onBulkEditOverflowClicked()

        assertEquals(
            navigationEventTracker.lastValue,
            MyListNavigationEvent.ShowBulkEditOverflowBottomSheet(listOf(testItem1))
        )
    }

    @Test
    fun `Bulk edit exits when clicking anything`() {
        subject.onEditChipClicked()
        subject.onMyListChipClicked()
        assertBulkEditCleared()

        subject.onEditChipClicked()
        subject.onArchiveChipClicked()
        assertBulkEditCleared()
        verify(exactly = 0) { listManager.setStatusFilter(ListStatus.ARCHIVE) }

        subject.onEditChipClicked()
        subject.onAllChipClicked()
        assertBulkEditCleared()
        verify(exactly = 0) { listManager.clearFilters() }

        subject.onEditChipClicked()
        subject.onTaggedChipClicked()
        assertBulkEditCleared()

        subject.onEditChipClicked()
        subject.onFavoritesChipClicked()
        assertBulkEditCleared()
        verify(exactly = 0) { listManager.addFilter(ItemFilterKey.FAVORITE) }

        subject.onEditChipClicked()
        subject.onEditChipClicked()
        assertBulkEditCleared()

        subject.onEditChipClicked()
        subject.onSearchClicked()
        assertBulkEditCleared()
        assertEquals(
            navigationEventTracker.sharedFlowUpdates.size,
            0
        )

        subject.onEditChipClicked()
        subject.onListenClicked()
        assertBulkEditCleared()
        assertEquals(
            navigationEventTracker.sharedFlowUpdates.size,
            0
        )

        subject.onEditChipClicked()
        subject.onFilterChipClicked()
        assertBulkEditCleared()
        assertEquals(
            navigationEventTracker.sharedFlowUpdates.size,
            0
        )

        subject.onEditChipClicked()
        subject.onBackButtonClicked()
        assertBulkEditCleared()

        subject.onEditChipClicked()
        subject.onBulkEditFinished()
        assertBulkEditCleared()
    }

    @Test
    fun `Tag badge clicked behavior`() {
        subject.onTagBadgeClicked("test")
        verify { listManager.setTag("test") }
    }

    @Test
    fun `pull to refresh behavior`() {
        // refreshing state
        subject.onPulledToRefresh()
        assertEquals(state.isRefreshing, true)
        verify { offlineDownloading.releaseAutoDownload() }
        verify { offlineDownloading.allowRetries() }
        verify { appSync.sync(any(), any(), null) }

        // successful refresh
        val onSuccess = slot<AppSync.SyncSuccess>()
        every { appSync.sync(capture(onSuccess), any(), null) } returns null
        subject.onPulledToRefresh()
        onSuccess.captured.onSyncCompleted()
        assertEquals(state.isRefreshing, false)

        // failed refresh
        val onFailed = slot<AppSync.SyncFail>()
        every { appSync.sync(any(), capture(onFailed), null) } returns null
        subject.onPulledToRefresh()
        val exception = Exception()
        onFailed.captured.onSyncFailed(exception)
        assertEquals(state.isRefreshing, false)
        assertEquals(
            navigationEventTracker.lastValue,
            MyListNavigationEvent.ShowSyncError(exception)
        )
    }

    @Test
    fun `item shared behavior`() {
        subject.onShareItemClicked(testItem1)
        assertEquals(
            navigationEventTracker.lastValue,
            MyListNavigationEvent.ShowShare(testItem1.toDomainItem())
        )
    }

    @Test
    fun `item overflow behavior`() {
        subject.onItemOverflowClicked(testItem1)
        assertEquals(
            navigationEventTracker.lastValue,
            MyListNavigationEvent.ShowItemOverflow(testItem1)
        )
    }

    private fun assertBulkEditCleared() {
        assertEquals(list.first().isSelectedForBulkEdit, false)
        assertEquals(state.bulkEditSnackBarText, "select all")
        assertEquals(state.bulkEditTextClickable, true)
        assertEquals(state.bulkEditActionsEnabled, false)
        assertEquals(state.editChipState, ChipState())
    }

    @Test
    fun `my list chip state`() {
        assertEquals(
            expected = ChipState(selected = true),
            actual = state.myListChipState
        )

        listManagerSortFilterState.edit {
            copy(
                listStatus = ListStatus.ARCHIVE
            )
        }

        assertEquals(
            expected = ChipState(selected = false),
            actual = state.myListChipState
        )
    }

    @Test
    fun `archive chip state`() {
        assertEquals(
            expected = ChipState(selected = false),
            actual = state.archiveChipState
        )

        listManagerSortFilterState.edit {
            copy(
                listStatus = ListStatus.ARCHIVE
            )
        }

        assertEquals(
            expected = ChipState(selected = true),
            actual = state.archiveChipState
        )
    }

    @Test
    fun `all chip state`() {
        assertEquals(
            expected = ChipState(selected = true),
            actual = state.allChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.FAVORITE)
            )
        }

        assertEquals(
            expected = ChipState(selected = false),
            actual = state.allChipState
        )
    }

    @Test
    fun `favorites chip state`() {
        assertEquals(
            expected = ChipState(selected = false),
            actual = state.favoritesChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.FAVORITE)
            )
        }

        assertEquals(
            expected = ChipState(selected = true),
            actual = state.favoritesChipState
        )
    }

    @Test
    fun `tagged chip state`() {
        assertEquals(
            expected = ChipState(selected = false),
            actual = state.taggedChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.TAG)
            )
        }

        assertEquals(
            expected = ChipState(selected = true),
            actual = state.taggedChipState
        )
    }

    @Test
    fun `highlighted chip state`() {
        assertEquals(
            expected = ChipState(selected = false),
            actual = state.highlightsChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.HIGHLIGHTED)
            )
        }

        assertEquals(
            expected = ChipState(selected = true),
            actual = state.highlightsChipState
        )
    }

    @Test
    fun `filter chip state`() {
        assertEquals(
            expected = ChipState(badgeVisible = false),
            actual = state.filterChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.VIEWED)
            )
        }

        assertEquals(
            expected = ChipState(badgeVisible = true),
            actual = state.filterChipState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.NOT_VIEWED)
            )
        }

        assertEquals(
            expected = ChipState(badgeVisible = true),
            actual = state.filterChipState
        )
    }

    @Test
    fun `selected tag chip state`() {
        assertEquals(
            expected = ChipState(
                selected = true,
                text = "",
                visibility = View.GONE
            ),
            actual = state.selectedTagChipState
        )

        listManagerSortFilterState.edit {
            copy(
                tag = "test"
            )
        }

        assertEquals(
            expected = ChipState(
                selected = true,
                text = "test",
                visibility = View.VISIBLE
            ),
            actual = state.selectedTagChipState
        )
    }

    @Test
    fun `not tagged filter chip states`() {
        every { stringLoader.getString(R.string.mu_untagged) } returns "not tagged"

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.NOT_TAGGED)
            )
        }

        assertEquals(
            expected = ChipState(
                selected = true,
                text = "not tagged",
                visibility = View.VISIBLE
            ),
            actual = state.selectedTagChipState
        )

        assertEquals(
            expected = ChipState(selected = true),
            actual = state.taggedChipState
        )
    }

    @Test
    fun `empty view state`() {
        assertEquals(
            expected = EmptyViewState.All,
            actual = state.emptyViewState
        )

        listManagerSortFilterState.edit {
            copy(
                tag = "test"
            )
        }

        assertEquals(
            expected = EmptyViewState.SpecificTag,
            actual = state.emptyViewState
        )

        listManagerSortFilterState.edit {
            copy(
                tag = null,
                listStatus = ListStatus.ARCHIVE
            )
        }

        assertEquals(
            expected = EmptyViewState.Archive,
            actual = state.emptyViewState
        )

        listManagerSortFilterState.edit {
            copy(
                listStatus = ListStatus.SAVES,
                filters = listOf(ItemFilterKey.TAG)
            )
        }

        assertEquals(
            expected = EmptyViewState.Tagged,
            actual = state.emptyViewState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.FAVORITE)
            )
        }

        assertEquals(
            expected = EmptyViewState.Favorite,
            actual = state.emptyViewState
        )

        listManagerSortFilterState.edit {
            copy(
                filters = listOf(ItemFilterKey.HIGHLIGHTED)
            )
        }

        assertEquals(
            expected = EmptyViewState.Highlights,
            actual = state.emptyViewState
        )
    }

    @Test
    fun `screen state`() {
        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.INITIAL }
        assertEquals(
            expected = MyListScreenState.Loading,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.INITIAL_LOADING }
        assertEquals(
            expected = MyListScreenState.Loading,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.INITIAL_ERROR }
        assertEquals(
            expected = MyListScreenState.Error,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.LOADED }
        assertEquals(
            expected = MyListScreenState.List,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.LOADED_APPENDING }
        assertEquals(
            expected = MyListScreenState.List,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.LOADED_APPEND_ERROR }
        assertEquals(
            expected = MyListScreenState.List,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.LOADED_REFRESHING }
        assertEquals(
            expected = MyListScreenState.List,
            actual = state.screenState
        )

        listManagerLoadStateFlow.edit { DataSourceCache.LoadState.LOADED_REFRESH_ERROR }
        assertEquals(
            expected = MyListScreenState.List,
            actual = state.screenState
        )
    }

    @Test
    fun `edit chip enabled`() {
        assertEquals(
            expected = ChipState(enabled = true),
            actual = state.editChipState
        )

        listManagerList.update { listOf<Any>() }

        assertEquals(
            expected = ChipState(enabled = false),
            actual = state.editChipState
        )
    }

    @Test
    fun `list item state`() {
        every {
            modelBindingHelper.title(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns HtmlString("title")
        every {
            modelBindingHelper.domain(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns HtmlString("domain")
        subject = MyListViewModel(
            listManager,
            itemRepository,
            undoable,
            modelBindingHelper,
            stringLoader,
            tagRepository,
            pocketCache,
            offlineDownloading,
            appSync,
            searchRepository,
            tracker,
            contentOpenTracker,
        )
        assertEquals(
            expected = "title",
            actual = list.first().title.value
        )
        assertEquals(
            expected = "domain",
            actual = list.first().domain.value
        )
        assertEquals(
            expected = false,
            actual = list.first().titleBold
        )
    }

    @Test
    fun `on favorite clicked`() {
        subject.onFavoriteClicked(testItem1)
        verify { itemRepository.toggleFavorite(testItem1) }
    }

    @Test
    fun `on item clicked`() {
        subject.onItemClicked(testItem1, 0)

        assertEquals(
            actual = navigationEventTracker.lastValue,
            expected = MyListNavigationEvent.GoToReader(testItem1, 0)
        )
    }

    @Test
    fun `on search clicked`() {
        subject.onSearchClicked()
        verify { listManager.isSearching = true }
        verify { listManager.clearFilters() }

        assertEquals(
            expected = MyListScreenState.SearchLanding,
            actual = state.screenState
        )

        assertEquals(
            expected = MyListNavigationEvent.SetSearchFocus,
            actual = navigationEventTracker.lastValue
        )
    }

    @Test
    fun `on close search clicked with no text in the searchbar`() {
        every { listManager.isSearching } returns true
        subject.onCloseSearchClicked()

        verify { listManager.isSearching = false }

        assertEquals(
            expected = MyListNavigationEvent.UpdateSearch(""),
            actual = navigationEventTracker.firstValue
        )

        assertEquals(
            expected = MyListNavigationEvent.CloseKeyboard,
            actual = navigationEventTracker.sharedFlowUpdates[1]
        )

        assertEquals(
            expected = MyListNavigationEvent.TrackSearchAnalytics(""),
            actual = navigationEventTracker.lastValue
        )
    }

    @Test
    fun `on close search clicked with text in the searchbar`() {
        every { listManager.sortFilterState.value.search } returns ""
        every { listManager.isSearching } returns true
        subject.onSearchTextChanged("test")
        subject.onCloseSearchClicked()

        assertEquals(
            expected = MyListNavigationEvent.UpdateSearch(""),
            actual = navigationEventTracker.firstValue
        )

        assertEquals(
            expected = MyListNavigationEvent.TrackSearchAnalytics(""),
            actual = navigationEventTracker.lastValue
        )

        assertEquals(
            expected = MyListScreenState.SearchLanding,
            actual = state.screenState
        )
    }

    @Test
    fun `on search text changed`() {
        subject.onSearchTextChanged("test", delayTime = 0)

        assertEquals(
            expected = MyListNavigationEvent.TrackSearchAnalytics("test"),
            actual = navigationEventTracker.firstValue
        )

        verify { listManager.setSearchText("test") }
    }

    @Test
    fun `on search done clicked`() {
        subject.onSearchDoneClicked()
        verify(exactly = 0) { searchRepository.addRecentSearch(any()) }

        subject.onSearchTextChanged("test")
        subject.onSearchDoneClicked()
        verify { searchRepository.addRecentSearch("test") }
        verify { listManager.setSearchText("test") }
    }

    @Test
    fun `on recent search clicked`() {
        subject.onRecentSearchClicked("test")

        assertEquals(
            expected = MyListNavigationEvent.UpdateSearch("test"),
            actual = navigationEventTracker.firstValue
        )

        assertEquals(
            expected = MyListNavigationEvent.CloseKeyboard,
            actual = navigationEventTracker.sharedFlowUpdates[1]
        )

        verify { listManager.setSearchText("test") }
    }

    @Test
    fun `WHEN an item is clicked THEN an analytics event is sent`() {
        val item = Item.Builder().given_url(UrlString("url")).build()
        subject.onItemClicked(item, 1)

        verify(exactly = 1) {
            contentOpenTracker.track(
                SavesEvents.savedCardContentOpen(
                    itemUrl = "url",
                    positionInList = 1,
                    savesTab = SavesTab.SAVES,
                )
            )
        }
    }

    @Test
    fun `WHEN add button is clicked THEN add url bottom sheet is shown`() = runTest {
        subject.navigationEvents.test {
            subject.onAddClicked()

            assertEquals(MyListNavigationEvent.ShowAddUrlBottomSheet, awaitItem())
        }
    }

    @Test
    fun `WHEN add button is clicked THEN an analytics event is sent`() {
        subject.onAddClicked()

        verify(exactly = 1) {
            tracker.track(SavesEvents.addButtonClicked())
        }
    }
}
