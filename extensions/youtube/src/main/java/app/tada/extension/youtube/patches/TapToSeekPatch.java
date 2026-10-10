package app.tada.extension.youtube.patches;

import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class TapToSeekPatch {
    public static boolean tapToSeekEnabled() {
        return Settings.TAP_TO_SEEK.get();
    }
}
