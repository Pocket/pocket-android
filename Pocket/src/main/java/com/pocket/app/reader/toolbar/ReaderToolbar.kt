package com.pocket.app.reader.toolbar

import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.tts.Track
import kotlinx.coroutines.flow.StateFlow

object ReaderToolbar {

    sealed class ToolbarEvent {

        data object GoBack : ToolbarEvent()
        data object GoToSignIn : ToolbarEvent()
        data class OpenListen(
            val track: Track?
        ) : ToolbarEvent()

        data class Share(
            val title: String
        ) : ToolbarEvent()

        data class ShowOverflow(
            val overflowUiState: ToolbarOverflowUiState
        ) : ToolbarEvent()

        data class ShowTagScreen(
            val item: Item?
        ) : ToolbarEvent()

        object ShowArticleReportedToast : ToolbarEvent()
    }

    interface ToolbarInteractions {
        fun onUpClicked()
        fun onAiClicked()
        fun onSaveClicked()
        fun onArchiveClicked()
        fun onReAddClicked()
        fun onListenClicked()
        fun onShareClicked()
        fun onOverflowClicked()
    }

    interface ToolbarOverflowInteractions {
        fun onTextSettingsClicked()
        fun onViewOriginalClicked()
        fun onRefreshClicked()
        fun onFindInPageClicked()
        fun onFavoriteClicked()
        fun onUnfavoriteClicked()
        fun onAddTagsClicked()
        fun onHighlightsClicked()
        fun onMarkAsNotViewedClicked()
        fun onDeleteClicked()
        fun onReportArticleClicked()
    }

    interface ToolbarUiStateHolder {
        val toolbarUiState: StateFlow<ToolbarUiState>
    }

    data class ToolbarUiState(
        val toolbarVisible: Boolean = false,
        val upVisible: Boolean = false,
        val aiButtonVisible: Boolean = false,
        val actionButtonState: ActionButtonState = ActionButtonState.None,
        val listenVisible: Boolean = false,
        val shareVisible: Boolean = false,
        val overflowVisible: Boolean = false,
    )

    sealed class ActionButtonState(
        val saveVisible: Boolean = false,
        val archiveVisible: Boolean = false,
        val reAddVisible: Boolean = false,
    ) {
        object None : ActionButtonState()
        class Save : ActionButtonState(
            saveVisible = true
        )
        class Archive : ActionButtonState(
            archiveVisible = true
        )
        class ReAdd : ActionButtonState(
            reAddVisible = true
        )
    }

    open class ToolbarOverflowUiState(
        val textSettingsVisible: Boolean = false,
        val viewOriginalVisible: Boolean = false,
        val refreshVisible: Boolean = false,
        val findInPageVisible: Boolean = false,
        val favoriteVisible: Boolean = false,
        val unfavoriteVisible: Boolean = false,
        val addTagsVisible: Boolean = false,
        val highlightsVisible: Boolean = false,
        val markAsNotViewedVisible: Boolean = false,
        val deleteVisible: Boolean = false,
        val reportArticleVisible: Boolean = false,
    )
}