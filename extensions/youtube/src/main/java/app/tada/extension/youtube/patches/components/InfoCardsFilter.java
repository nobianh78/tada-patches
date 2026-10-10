package app.tada.extension.youtube.patches.components;

import app.tada.extension.shared.patches.components.Filter;
import app.tada.extension.shared.patches.components.StringFilterGroup;

import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class InfoCardsFilter extends Filter {

    public InfoCardsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_INFO_CARDS,
                        "info_card_teaser_overlay.e"
                )
        );
    }
}
