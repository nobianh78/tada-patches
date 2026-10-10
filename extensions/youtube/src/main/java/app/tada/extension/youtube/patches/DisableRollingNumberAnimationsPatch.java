package app.tada.extension.youtube.patches;

import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DisableRollingNumberAnimationsPatch {
    /**
     * Injection point.
     */
    public static boolean disableRollingNumberAnimations() {
        return Settings.DISABLE_ROLLING_NUMBER_ANIMATIONS.get();
    }
}
