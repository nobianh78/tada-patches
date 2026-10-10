package app.tada.extension.youtube.patches;

import app.tada.extension.shared.Logger;
import app.tada.extension.shared.Utils;
import app.tada.extension.shared.settings.SharedYouTubeSettings;
import app.tada.extension.shared.settings.preference.ExternalDownloaderPreference;

@SuppressWarnings("unused")
public final class DownloadsPatch {

    /**
     * Injection point.
     * <p>
     * Called from the in-app download hook,
     * for both the player action button (below the video)
     * and the 'Download video' flyout option for feed videos.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppDownloadButtonOnClick(String videoId) {
        try {
            if (!SharedYouTubeSettings.EXTERNAL_DOWNLOADER_ACTION_BUTTON.get()) {
                return false;
            }

            ExternalDownloaderPreference.launchExternalDownloader(
                    videoId, Utils.getActivity(), "https://youtu.be/" + videoId);
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "inAppDownloadButtonOnClick failure", ex);
        }
        return false;
    }

}
