/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.extension.youtube.patches.components;

import app.tada.extension.shared.patches.components.BufferAsciiStrings;
import app.tada.extension.shared.patches.components.ByteArrayFilterGroup;
import app.tada.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.tada.extension.shared.patches.components.ContextInterface;
import app.tada.extension.shared.patches.components.Filter;
import app.tada.extension.shared.patches.components.StringFilterGroup;
import app.tada.extension.youtube.patches.LayoutReloadObserverPatch;
import app.tada.extension.youtube.settings.Settings;
import app.tada.extension.youtube.shared.EngagementPanel;
import app.tada.extension.youtube.shared.NavigationBar;
import app.tada.extension.youtube.shared.NavigationBar.NavigationButton;
import app.tada.extension.youtube.shared.PlayerType;
import app.tada.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public final class HorizontalShelvesFilter extends Filter {
    private final ByteArrayFilterGroupList descriptionBuffers = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroupList generalBuffers = new ByteArrayFilterGroupList();

    public HorizontalShelvesFilter() {
        StringFilterGroup horizontalShelves = new StringFilterGroup(
                null,
                "horizontal_shelf.e"
        );

        addPathCallbacks(horizontalShelves);

        descriptionBuffers.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ATTRIBUTES_SECTION,
                        "cell_video_attribute"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUIZZES_SECTION,
                        "post_base_wrapper_slim"
                )
        );

        generalBuffers.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_CREATOR_STORE_SHELF,
                        "shopping_item_card_list"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_HISTORY_SHELF,
                        // Browse id of the History page, opened by the shelf header.
                        "FEhistory"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_MOVIES_SECTION,
                        "movie_card.e"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PLAYABLES,
                        "FEmini_app_destination"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_TICKET_SHELF,
                        "ticket_item.e"
                )
        );
    }

    private boolean hideShelves(ContextInterface contextInterface) {
        if (!Settings.HIDE_HORIZONTAL_SHELVES.get() || isPlayerOrDescription()) {
            return false;
        }
        return contextInterface.isHomeFeedOrRelatedVideo()
                || NavigationBar.isSearchBarActive()
                || NavigationBar.isBackButtonVisible()
                || NavigationButton.getSelectedNavigationButton() != NavigationButton.LIBRARY;
    }

    private boolean isPlayerOrDescription() {
        return EngagementPanel.isDescription()
                || PlayerType.getCurrent().isMaximizedOrFullscreen()
                || LayoutReloadObserverPatch.isActionBarVisible.get()
                || ShortsPlayerState.isOpen();
    }

    @Override
    public boolean isFiltered(ContextInterface contextInterface,
                              String identifier,
                              String accessibility,
                              CharSequence path,
                              byte[] buffer,
                              BufferAsciiStrings asciiStrings,
                              StringFilterGroup matchedGroup,
                              FilterContentType contentType,
                              int contentIndex) {
        if (contentIndex != 0) {
            return false;
        }
        if (generalBuffers.check(buffer).isFiltered()) {
            return true;
        }
        if (descriptionBuffers.check(buffer).isFiltered()) {
            if (isPlayerOrDescription()) {
                return true;
            }
        }
        return hideShelves(contextInterface);
    }
}
