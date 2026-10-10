package app.tada.extension.youtube.patches;

import android.view.ViewGroup;

import app.tada.extension.youtube.shared.PlayerOverlays;

@SuppressWarnings("unused")
public class PlayerOverlaysHookPatch {
    /**
     * Injection point.
     */
    public static void playerOverlayInflated(ViewGroup group) {
        PlayerOverlays.attach(group);
    }
}
