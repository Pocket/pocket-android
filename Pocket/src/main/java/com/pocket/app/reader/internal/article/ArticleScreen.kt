package com.pocket.app.reader.internal.article

import com.pocket.app.reader.queue.QueueManager
import com.pocket.data.models.ArticleImage
import com.pocket.data.models.DomainItem

class ArticleScreen {

    interface Initializer {
        fun onInitialized(url: String)
    }

    interface LifecycleCallbacks {
        fun onPaused(currentScrollPosition: Int)
        fun onResume()
    }

    interface EndOfArticleRecommendationInteractions {
        fun onCardClicked(url: String, corpusRecommendationId: String)
        fun onSaveClicked(url: String, isSaved: Boolean, corpusRecommendationId: String)
        fun onOverflowClicked(url: String, title: String, corpusRecommendationId: String?)
        fun onArticleViewed(position: Int, url: String, corpusRecommendationId: String)
    }

    interface WebViewCallbacks {
        fun onInitialPageLoaded(theme: Int, density: Float)
        fun onActionModeHighlightClicked()
        fun onHighlightPatchRequested(patch: String, text: String)
        fun onArticleHtmlLoadedIntoWebView(screenWidth: Int)
        fun onInternalLinkClicked(url: String)
        fun onArticleLinkOpened(url: String)
    }

    interface LongPressDialogInteractions {
        fun onSaveClicked(url: String)
    }

    // callbacks from the highlight bottom sheet fragment
    interface HighlightOverlayCallbacks {
        fun onHighlightDeleted()
        fun onHighlightClickedFromOverlay(highlightId: String)
    }

    interface ErrorInteractions {
        fun onRetryClicked()
        fun onJavascriptError()
    }

    interface FindTextToolbarInteractions {
        fun onNextClicked()
        fun onPreviousClicked()
        fun onShow()
        fun onCloseClicked()
        fun onTextChanged(text: String)
        fun onTextHighlighted(count: Int)
    }

    sealed class Event {
        data object GoBack : Event()
        data object GoToOriginalWebView : Event()
        data object GoToAi : Event()
        data object GoToSignIn : Event()
        data object ShowHighlightsUpsell : Event()
        data object ShowHighlightBottomSheet : Event()
        data class ShowShare(
            val item: DomainItem,
            val quote: String?,
        ) : Event()
        data class ExecuteJavascript(
            val command: String,
        ) : Event()
        object ShowTextSettingsBottomSheet : Event()
        object ShowTextFinder : Event()
        data class OpenNewUrl(
            val url: String,
            val queueManager: QueueManager,
        ) : Event()
        data class OpenOverflowBottomSheet(
            val url: String,
            val title: String,
            val corpusRecommendationId: String?,
        ) : Event()
        data class ScrollToSavedPosition(
            val position: Int
        ) : Event()
        data class OpenImage(
            val articleImages: List<ArticleImage>,
            val startingId: Int,
        ) : Event()
        object ShowSavedToast : Event()
        object ShowArchivedToast : Event()
        object ShowReAddedToast : Event()
    }
}