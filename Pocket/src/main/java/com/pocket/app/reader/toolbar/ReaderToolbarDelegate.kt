package com.pocket.app.reader.toolbar

import android.util.Log
import com.pocket.data.models.DomainItem
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ReaderToolbarEvents
import com.pocket.repository.ArticleRepository
import com.pocket.repository.ItemRepository
import com.pocket.sdk.tts.toTrack
import com.pocket.usecase.Save
import com.pocket.util.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * For use within a view model that uses the ReaderToolbarView.
 * Contains a lot of interaction function logic
 */
open class ReaderToolbarDelegate(
    private val itemRepository: ItemRepository,
    private val articleRepository: ArticleRepository,
    private val save: Save,
    private val coroutineScope: CoroutineScope,
    private val tracker: Tracker,
) : ReaderToolbar.ToolbarInteractions,
    ReaderToolbar.ToolbarOverflowInteractions,
    ReaderToolbar.ToolbarUiStateHolder {

    lateinit var url: String

    protected val _toolbarEvents = MutableSharedFlow<ReaderToolbar.ToolbarEvent>(extraBufferCapacity = 1)
    val toolbarEvents: SharedFlow<ReaderToolbar.ToolbarEvent> = _toolbarEvents

    protected val _toolbarUiState = MutableStateFlow(ReaderToolbar.ToolbarUiState())
    override val toolbarUiState: StateFlow<ReaderToolbar.ToolbarUiState> = _toolbarUiState

    open suspend fun getToolbarOverflow(): ReaderToolbar.ToolbarOverflowUiState = ReaderToolbar.ToolbarOverflowUiState()

    override fun onUpClicked() {
        tracker.track(ReaderToolbarEvents.upClicked())
        _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.GoBack)
    }

    override fun onAiClicked() {
    }

    override fun onSaveClicked() {
        tracker.track(ReaderToolbarEvents.saveClicked(url))
        coroutineScope.launch {
            when (save(url)) {
                Save.Result.Success -> {
                    _toolbarUiState.edit { copy(
                        actionButtonState = ReaderToolbar.ActionButtonState.Archive()
                    ) }
                }
                Save.Result.NotLoggedIn -> {
                    _toolbarEvents.emit(ReaderToolbar.ToolbarEvent.GoToSignIn)
                }
            }
        }
    }

    override fun onArchiveClicked() {
        tracker.track(ReaderToolbarEvents.archiveClicked())
        itemRepository.archive(url)
        _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.GoBack)
    }

    override fun onReAddClicked() {
        tracker.track(ReaderToolbarEvents.reAddClicked())
        itemRepository.unArchive(url)
        _toolbarUiState.edit { copy(
            actionButtonState = ReaderToolbar.ActionButtonState.Archive()
        ) }
    }

    override fun onListenClicked() {
        tracker.track(ReaderToolbarEvents.listenClicked())
        coroutineScope.launch {
            _toolbarEvents.tryEmit(
                ReaderToolbar.ToolbarEvent.OpenListen(getItem()?.toTrack())
            )
        }
    }

    override fun onShareClicked() {
        tracker.track(ReaderToolbarEvents.shareClicked())
        coroutineScope.launch {
            val item = getDomainItem()
            _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.Share(item?.displayTitle ?: ""))
        }
    }

    override fun onOverflowClicked() {
        tracker.track(ReaderToolbarEvents.overflowClicked())
        coroutineScope.launch {
            _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.ShowOverflow(getToolbarOverflow()))
        }
    }

    override fun onTextSettingsClicked() {
        tracker.track(ReaderToolbarEvents.textSettingsClicked())
    }

    override fun onViewOriginalClicked() {
        tracker.track(ReaderToolbarEvents.viewOriginalClicked())
    }

    override fun onRefreshClicked() {
        tracker.track(ReaderToolbarEvents.refreshClicked())
    }

    override fun onFindInPageClicked() {
        tracker.track(ReaderToolbarEvents.findInPageClicked())
    }

    override fun onFavoriteClicked() {
        tracker.track(ReaderToolbarEvents.favoriteClicked())
        itemRepository.favorite(url)
    }

    override fun onUnfavoriteClicked() {
        tracker.track(ReaderToolbarEvents.unfavoriteClicked())
        itemRepository.unfavorite(url)
    }

    override fun onAddTagsClicked() {
        tracker.track(ReaderToolbarEvents.addTagsClicked())
        coroutineScope.launch {
            _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.ShowTagScreen(getItem()))
        }
    }

    override fun onHighlightsClicked() {
        tracker.track(ReaderToolbarEvents.highlightsClicked())
    }

    override fun onMarkAsNotViewedClicked() {
        tracker.track(ReaderToolbarEvents.markAsViewedClicked())
        itemRepository.markAsNotViewed(url)
        _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.GoBack)
    }

    override fun onDeleteClicked() {
        tracker.track(ReaderToolbarEvents.deleteClicked())
        itemRepository.delete(url)
        _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.GoBack)
    }

    override fun onReportArticleClicked() {
        tracker.track(ReaderToolbarEvents.reportClicked())
        articleRepository.reportArticle(url)
        _toolbarEvents.tryEmit(ReaderToolbar.ToolbarEvent.ShowArticleReportedToast)
    }

    suspend fun getDomainItem(): DomainItem? =
        try {
            itemRepository.getDomainItem(url)
        } catch (e: Exception) {
            Log.e("Reader", e.message ?: "")
            null
        }

    private suspend fun getItem(): com.pocket.sdk.api.generated.thing.Item? =
        try {
            itemRepository.getItemOrThrow(url)
        } catch (e: Exception) {
            Log.e("Reader", e.message ?: "")
            null
        }
}