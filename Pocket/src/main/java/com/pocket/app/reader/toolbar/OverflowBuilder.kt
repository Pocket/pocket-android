package com.pocket.app.reader.toolbar

import android.view.View
import com.ideashower.readitlater.R
import com.pocket.ui.view.menu.MenuItem
import com.pocket.ui.view.menu.ThemedPopupMenu

object OverflowBuilder {

    fun showOverflow(
        anchorView: View,
        overflowUiState: ReaderToolbar.ToolbarOverflowUiState,
        toolbarInteractions: ReaderToolbar.ToolbarOverflowInteractions,
    ) {
        val menuItems = mutableListOf<MenuItem>()

        if (overflowUiState.textSettingsVisible) menuItems.add(
            MenuItem(
                R.string.mu_display_settings,
                com.pocket.ui.R.drawable.ic_pkt_text_style_solid
            ) {
                toolbarInteractions.onTextSettingsClicked()
            }
        )

        if (overflowUiState.viewOriginalVisible) menuItems.add(
            MenuItem(
                R.string.mu_view_original,
                com.pocket.ui.R.drawable.ic_pkt_web_view_line
            ) {
                toolbarInteractions.onViewOriginalClicked()
            }
        )

        if (overflowUiState.refreshVisible) menuItems.add(
            MenuItem(
                R.string.mu_refresh,
                com.pocket.ui.R.drawable.ic_pkt_refresh_line
            ) {
                toolbarInteractions.onRefreshClicked()
            }
        )

        if (overflowUiState.findInPageVisible) menuItems.add(
            MenuItem(
                R.string.mu_find_in_page,
                com.pocket.ui.R.drawable.ic_pkt_search_line
            ) {
                toolbarInteractions.onFindInPageClicked()
            }
        )

        if (overflowUiState.favoriteVisible) menuItems.add(
            MenuItem(
                R.string.mu_favorite,
                com.pocket.ui.R.drawable.ic_pkt_favorite_line
            ) {
                toolbarInteractions.onFavoriteClicked()
            }
        )

        if (overflowUiState.unfavoriteVisible) menuItems.add(
            MenuItem(
                R.string.mu_unfavorite,
                com.pocket.ui.R.drawable.ic_pkt_favorite_solid
            ) {
                toolbarInteractions.onUnfavoriteClicked()
            }
        )

        if (overflowUiState.addTagsVisible) menuItems.add(
            MenuItem(
                R.string.mu_add_tags,
                com.pocket.ui.R.drawable.ic_pkt_add_tags_line
            ) {
                toolbarInteractions.onAddTagsClicked()
            }
        )

        if (overflowUiState.highlightsVisible) menuItems.add(
            MenuItem(
                R.string.mu_annotations,
                com.pocket.ui.R.drawable.ic_pkt_highlights_line
            ) {
                toolbarInteractions.onHighlightsClicked()
            }
        )

        if (overflowUiState.markAsNotViewedVisible) menuItems.add(
            MenuItem(
                com.pocket.ui.R.string.ic_mark_as_not_viewed,
                com.pocket.ui.R.drawable.ic_viewed_not
            ) {
                toolbarInteractions.onMarkAsNotViewedClicked()
            }
        )

        if (overflowUiState.deleteVisible) menuItems.add(
            MenuItem(
                R.string.mu_delete,
                com.pocket.ui.R.drawable.ic_pkt_delete_line
            ) {
                toolbarInteractions.onDeleteClicked()
            }
        )

        if (overflowUiState.reportArticleVisible) menuItems.add(
            MenuItem(
                R.string.mu_report_article_view,
                com.pocket.ui.R.drawable.ic_pkt_error_line
            ) {
                toolbarInteractions.onReportArticleClicked()
            }
        )

        ThemedPopupMenu(
            anchorView.context,
            ThemedPopupMenu.Section.actions(null, menuItems)
        ).show(anchorView)
    }
}