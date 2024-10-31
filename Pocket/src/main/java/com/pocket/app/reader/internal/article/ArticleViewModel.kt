package com.pocket.app.reader.internal.article

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocket.analytics.ContentOpenTracker
import com.pocket.analytics.Tracker
import com.pocket.analytics.appevents.ArticleViewEvents
import com.pocket.app.premium.PremiumFonts
import com.pocket.app.reader.internal.article.DisplaySettingsManager.OnDisplaySettingsChangedListener
import com.pocket.app.reader.internal.article.javascript.JavascriptFunctions
import com.pocket.app.reader.toolbar.ReaderToolbar
import com.pocket.app.reader.toolbar.ReaderToolbarDelegate
import com.pocket.data.models.ArticleImage
import com.pocket.data.models.DomainItem
import com.pocket.data.models.articlePosition
import com.pocket.data.models.toDomainItem
import com.pocket.repository.ArticleRepository
import com.pocket.repository.HighlightRepository
import com.pocket.repository.ItemRepository
import com.pocket.sdk.api.generated.enums.PremiumFeature
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.value.UrlString
import com.pocket.sdk2.api.legacy.PocketCache
import com.pocket.usecase.Save
import com.pocket.util.android.FormFactor
import com.pocket.util.edit
import com.pocket.util.java.StopWatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val itemRepository: ItemRepository,
    private val save: Save,
    private val pocketCache: PocketCache,
    private val highlightRepository: HighlightRepository,
    private val displaySettingsManager: DisplaySettingsManager,
    private val premiumFonts: PremiumFonts,
    private val tracker: Tracker,
    private val contentOpenTracker: ContentOpenTracker,
) : ViewModel(),
    ArticleScreen.Initializer,
    ArticleScreen.ErrorInteractions,
    ArticleScreen.WebViewCallbacks,
    ArticleScreen.HighlightOverlayCallbacks,
    ArticleScreen.LifecycleCallbacks,
    OnDisplaySettingsChangedListener,
    ArticleScreen.LongPressDialogInteractions {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _events = MutableSharedFlow<ArticleScreen.Event>(extraBufferCapacity = 20)
    val events: SharedFlow<ArticleScreen.Event> = _events

    lateinit var url: String
    var images: MutableList<ArticleImage> = mutableListOf()

    // stopwatch for tracking how long a user spends viewing the article
    // used for the "Continue Reading" feature
    private val stopWatch = StopWatch()

    val toolbar = Toolbar()

    init {
        displaySettingsManager.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        displaySettingsManager.removeListener(this)
    }

    override fun onInitialized(url: String) {
        this.url = url
        toolbar.setupToolbar(url)
    }

    override fun onInitialPageLoaded(theme: Int, density: Float) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.load(
                displaySettingsManager,
                theme,
                density,
                FormFactor.getClassKey(false),
                Build.VERSION.SDK_INT
            )
        ))

        if (premiumFonts.fontsReady()) {
            premiumFonts.fontCssPaths.forEach { fontPath ->
                _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.addCustomCss(fontPath)
                ))
            }
        } else {
            premiumFonts.initFonts(false)
        }

        loadArticleHtml()
    }

    @Suppress("MagicNumber")
    private fun loadArticleHtml(forceRefresh: Boolean = false) {
        _uiState.edit { copy(
            screenState = ScreenState.Loading
        ) }
        viewModelScope.launch {
            try {
                _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.loadCallback(articleRepository.getArticleHtml(url, forceRefresh))
                ))
                // slight delay to give the webView time to load the article so we don't
                // see a white flash when the loading happens
                delay(200)
                _uiState.edit { copy(
                    screenState = ScreenState.Default
                ) }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", e.message ?: "")
                _uiState.edit { copy(
                    screenState = ScreenState.Error
                ) }
            }
        }
    }

    override fun onArticleHtmlLoadedIntoWebView(screenWidth: Int) {
        viewModelScope.launch {
            // some kind of race condition that I can't figure out
            // images and scroll don't seem to work sometimes
            // I tried posting to the main thread, but only a delay seems to work
            delay(100)

            applyHighlights()

            // scroll to correct position
            try {
                val item = itemRepository.getDomainItem(url)
                _events.tryEmit(ArticleScreen.Event.ScrollToSavedPosition(item.articlePosition?.scrollPosition ?: 0))
            } catch (ignored: Exception) {}

            // load videos
            articleRepository.getVideoJson(url).forEach { videoJson ->
                _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.loadVideo(videoJson.toString())
                ))
            }

            // load images
            articleRepository.getImages(
                url,
                screenWidth
            ).collect { articleImage ->
                images.add(articleImage)
                _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.loadImage(articleImage)
                ))
                _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                    JavascriptFunctions.requestContentHeight()
                ))
            }
        }
    }

    override fun onInternalLinkClicked(url: String) {
        val realUrl = url.replace("isril:", "").replace("ISRIL:", "")
        when {
            realUrl.startsWith("IMG") || realUrl.startsWith("LINKIMG") -> {
                val id = realUrl.split("||")[1].toInt()
                _events.tryEmit(ArticleScreen.Event.OpenImage(images, id))
            }
            realUrl.startsWith("LOGIN") -> {
                _events.tryEmit(ArticleScreen.Event.GoToOriginalWebView)
            }
        }
    }

    fun onActionModeShareClicked(selectedText: String?) {
        viewModelScope.launch {
            val item = itemRepository.getItem(url)
                ?: Item.Builder().given_url(UrlString(url)).build()
            _events.emit(ArticleScreen.Event.ShowShare(item.toDomainItem(), selectedText))
        }
    }

    override fun onActionModeHighlightClicked() {
        viewModelScope.launch {
            try {
                val item = try {
                    itemRepository.getDomainItem(url)
                } catch (e: Exception) {
                    null
                }
                if (pocketCache.hasFeature(PremiumFeature.ANNOTATIONS)
                    || (item != null && item.highlights.size < pocketCache.annotations_per_article_limit())
                ) {
                    _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
                        JavascriptFunctions.requestAnnotationPatch()
                    ))
                } else if (item != null) {
                    _events.tryEmit(ArticleScreen.Event.ShowHighlightsUpsell)
                }
            } catch (e: Exception) {
                Log.e("ArticleView", e.message ?: "")
            }
        }
    }

    /**
     * called when we get a javascript callback that gives us the info we need
     * to create a new highlight
     */
    override fun onHighlightPatchRequested(patch: String, text: String) {
        viewModelScope.launch {
            highlightRepository.addHighlight(
                patch = patch,
                text = text,
                itemUrl = url,
            )
            applyHighlights()
        }
    }

    /**
     * deletion happens in the bottom sheet's view model
     * We just need to refresh the displayed highlights
     */
    override fun onHighlightDeleted() {
        viewModelScope.launch { applyHighlights() }
    }

    override fun onHighlightClickedFromOverlay(highlightId: String) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.scrollToHighlight(highlightId)
        ))
    }

    /**
     * Highlight all the text that the user has created highlights for
     * Call this whenever we need to refresh the highlights
     */
    private suspend fun applyHighlights() {
        val highlights = try {
            itemRepository.getDomainItem(url).highlights
        } catch (e: Exception) {
            emptyList()
        }
        val highlightsJson = Json.encodeToString(highlights)
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.highlightAnnotations(highlightsJson)
        ))
    }

    override fun onRetryClicked() {
        loadArticleHtml()
    }

    override fun onJavascriptError() {
        _uiState.edit { copy(
            screenState = ScreenState.Error
        ) }
    }

    override fun onPaused(currentScrollPosition: Int) {
        stopWatch.pause()
        itemRepository.setScrollPosition(
            url,
            currentScrollPosition,
            (stopWatch.extract() / 1_000).toInt()
        )
    }

    override fun onResume() {
        stopWatch.resume()
    }

    inner class Toolbar : ReaderToolbarDelegate(
        articleRepository = articleRepository,
        itemRepository = itemRepository,
        save = save,
        coroutineScope = viewModelScope,
        tracker = tracker,
    ) {

        fun setupToolbar(url: String) {
            this.url = url
            viewModelScope.launch {
                val item: DomainItem? = getDomainItem()

                _toolbarUiState.edit {
                    when {
                        item?.isSaved ?: false -> {
                            ArticleToolbarState(
                                when {
                                    item?.isArchived
                                        ?: false -> ReaderToolbar.ActionButtonState.ReAdd()
                                    else -> ReaderToolbar.ActionButtonState.Archive()
                                }
                            )
                        }
                        else -> UnsavedSyndicatedArticleToolbarState()
                    }
                }
            }
        }

        override suspend fun getToolbarOverflow(): ReaderToolbar.ToolbarOverflowUiState {
            val item = getDomainItem()
            val httpUrl = url.toHttpUrl()

            return when {
                item?.isSaved ?: false && httpUrl.host == "getpocket.com" ->
                    SyndicatedArticleOverflowState(item!!.isFavorited)
                item?.isSaved ?: false -> ArticleOverflowState(item!!.isFavorited)
                else -> UnsavedSyndicatedArticleOverflowState()
            }
        }

        override fun onAiClicked() {
            super.onAiClicked()
            _events.tryEmit(ArticleScreen.Event.GoToAi)
        }

        override fun onViewOriginalClicked() {
            super.onViewOriginalClicked()
            _events.tryEmit(ArticleScreen.Event.GoToOriginalWebView)
        }

        override fun onRefreshClicked() {
            super.onRefreshClicked()
            loadArticleHtml(forceRefresh = true)
        }

        override fun onHighlightsClicked() {
            super.onHighlightsClicked()
            _events.tryEmit(ArticleScreen.Event.ShowHighlightBottomSheet)
        }

        override fun onTextSettingsClicked() {
            super.onTextSettingsClicked()
            _events.tryEmit(ArticleScreen.Event.ShowTextSettingsBottomSheet)
        }

        override fun onFindInPageClicked() {
            super.onFindInPageClicked()
            _events.tryEmit(ArticleScreen.Event.ShowTextFinder)
        }

        override fun onSaveClicked() {
            super.onSaveClicked()
            _events.tryEmit(ArticleScreen.Event.ShowSavedToast)
        }

        override fun onArchiveClicked() {
            super.onArchiveClicked()
            _events.tryEmit(ArticleScreen.Event.ShowArchivedToast)
        }

        override fun onReAddClicked() {
            super.onReAddClicked()
            _events.tryEmit(ArticleScreen.Event.ShowReAddedToast)
        }
    }

    override fun onFontChanged(fontChoice: Int) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newFontType(fontChoice)
        ))
    }

    override fun onFontSizeChanged(size: Int, isMinimum: Boolean, isMaximum: Boolean) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newFontSize(size)
        ))
    }

    override fun onLineHeightChanged(value: Int, isMinimum: Boolean, isMaximum: Boolean) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newLineHeightSetting(value)
        ))
    }

    override fun onMarginChanged(value: Int, isMinimum: Boolean, isMaximum: Boolean) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newMarginSetting(value)
        ))
    }

    override fun onTextAlignChanged(justify: Boolean) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newTextAlign(justify)
        ))
    }

    override fun onThemeChanged(theme: Int) {
        _events.tryEmit(ArticleScreen.Event.ExecuteJavascript(
            JavascriptFunctions.newTextStyle(theme)
        ))
    }

    override fun onSaveClicked(url: String) {
        viewModelScope.launch {
            when (save(url)) {
                Save.Result.Success -> _events.emit(ArticleScreen.Event.ShowSavedToast)
                Save.Result.NotLoggedIn -> _events.emit(ArticleScreen.Event.GoToSignIn)
            }
        }
    }

    override fun onArticleLinkOpened(url: String) {
        contentOpenTracker.track(ArticleViewEvents.articleLinkContentOpen(url))
    }

    override fun onBrightnessChanged(brightness: Float) = Unit

    data class UiState(
        val screenState: ScreenState = ScreenState.Loading,
    )

    sealed class ScreenState(
        val loadingVisible: Boolean = false,
        val mainLayoutVisible: Boolean = true, // always visible or bad things happen
        val errorVisible: Boolean = false,
    ) {
        object Loading : ScreenState(
            loadingVisible = true
        )
        object Error : ScreenState(
            errorVisible = true
        )
        object Default : ScreenState()
    }

    private fun ArticleToolbarState(
        actionButtonState: ReaderToolbar.ActionButtonState
    ) = ReaderToolbar.ToolbarUiState(
        toolbarVisible = true,
        upVisible = true,
        aiButtonVisible = true,
        actionButtonState = actionButtonState,
        listenVisible = true,
        shareVisible = true,
        overflowVisible = true,
    )

    private fun UnsavedSyndicatedArticleToolbarState() = ReaderToolbar.ToolbarUiState(
        toolbarVisible = true,
        upVisible = true,
        aiButtonVisible = true,
        actionButtonState = ReaderToolbar.ActionButtonState.Save(),
        listenVisible = true,
        shareVisible = true,
        overflowVisible = true,
    )

    private fun ArticleOverflowState(
        isFavorited: Boolean,
    ) = ReaderToolbar.ToolbarOverflowUiState(
        textSettingsVisible = true,
        viewOriginalVisible = true,
        refreshVisible = true,
        findInPageVisible = true,
        favoriteVisible = !isFavorited,
        unfavoriteVisible = isFavorited,
        addTagsVisible = true,
        highlightsVisible = true,
        markAsNotViewedVisible = true,
        deleteVisible = true,
        reportArticleVisible = true,
    )

    private fun SyndicatedArticleOverflowState(
        isFavorited: Boolean,
    ) = ReaderToolbar.ToolbarOverflowUiState(
        textSettingsVisible = true,
        refreshVisible = true,
        findInPageVisible = true,
        favoriteVisible = !isFavorited,
        unfavoriteVisible = isFavorited,
        addTagsVisible = true,
        highlightsVisible = true,
        markAsNotViewedVisible = true,
        deleteVisible = true,
        reportArticleVisible = true,
    )

    private fun UnsavedSyndicatedArticleOverflowState() = ReaderToolbar.ToolbarOverflowUiState(
        textSettingsVisible = true,
        refreshVisible = true,
        findInPageVisible = true,
        reportArticleVisible = true,
    )
}