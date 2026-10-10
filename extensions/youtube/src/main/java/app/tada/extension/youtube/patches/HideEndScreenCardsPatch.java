package app.tada.extension.youtube.patches;

import android.view.View;

import app.tada.extension.shared.Utils;
import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class HideEndScreenCardsPatch {

    /**
     * Injection point.
     */
    public static void hideEndScreenCardView(View view) {
        Utils.hideViewUnderCondition(Settings.HIDE_END_SCREEN_CARDS, view);
    }

    /**
     * Injection point.
     */
    public static boolean hideEndScreenCards() {
        return Settings.HIDE_END_SCREEN_CARDS.get();
    }
}
