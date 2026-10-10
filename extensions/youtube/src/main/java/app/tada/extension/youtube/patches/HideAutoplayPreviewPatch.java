package app.tada.extension.youtube.patches;

import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class HideAutoplayPreviewPatch {
    /**
     * Injection point.
     */
    public static boolean hideAutoplayPreview() {
        return Settings.HIDE_AUTOPLAY_PREVIEW.get();
    }
}
