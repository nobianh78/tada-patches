/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches/pull/3397
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.extension.youtube.videoplayer;

import android.view.View;

import app.tada.extension.shared.Logger;
import app.tada.extension.youtube.patches.PipButtonPatch;
import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class PipButton {

    static {
        if (Settings.PIP_BUTTON_OVERLAY.get() && PipButtonPatch.isPipSupported()) {
            LegacyPlayerControlButton.incrementUpperButtonCount();
        }
    }

    /**
     * Injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            new LegacyPlayerControlButton(
                    controlsView,
                    "tada_pip_button",
                    null,
                    "tada_pip_button",
                    () -> (Settings.PIP_BUTTON_OVERLAY.get() && PipButtonPatch.isPipSupported())
                            ? LegacyPlayerControlButton.ButtonVisibility.ENABLED
                            : LegacyPlayerControlButton.ButtonVisibility.DISABLED,
                    v -> PipButtonPatch.enterPictureInPicture(),
                    null
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }
}
