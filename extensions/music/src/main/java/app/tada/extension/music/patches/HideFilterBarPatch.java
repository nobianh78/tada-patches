package app.tada.extension.music.patches;

import android.view.View;

import app.tada.extension.music.settings.Settings;
import app.tada.extension.shared.Utils;

@SuppressWarnings("unused")
public class HideFilterBarPatch {

    /**
     * Injection point
     */
    public static void hideFilterBar(View view) {
        Utils.hideViewBy0dpUnderCondition(Settings.HIDE_FILTER_BAR, view);
    }
}
