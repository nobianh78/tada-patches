/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.feed;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Hide 'Are you interested?' bubbles, a switch of the "Hide suggested posts" patch.
 *
 * <p>Instagram's Home feed adapter adds a row of its own after some posts: a bubble asking "Are you
 * interested in this post?" with Not interested and Interested buttons. The server sends it, so the
 * words are in no resource of the app. The adapter's row builder adds it once a post carries the
 * question and the feed is Home's (the feed_timeline module, or its Older Posts page). Home can also
 * be built by the feed item binder group, which adds the same row when the server styles the
 * question UNIFIED_NO_SUBTEXT, without a subtitle (#131). The patch asks {@link #hide} right before both adds, and a yes
 * skips the add, so the row is never added and no blank one is left. Nothing here answers the
 * question for you.
 *
 * <p>The hook fails open: until the settings are ready, while HushGram is paused, with the switch
 * off, or if anything throws, it answers no and Instagram adds its row. Only the yes is counted, in
 * Diagnostics under "Hide suggested posts".
 */
public final class FeedInterest {
    /** Diagnostics count, fixed text. */
    static final String COUNT_HIDDEN = "interest questions hidden";

    private FeedInterest() {
    }

    /** True leaves the question's row out of Home. Never throws. */
    public static boolean hide() {
        try {
            HookStatus.invoked(FamilyNames.FEED_SUGGESTIONS);
            if (!Utils.settingsReady() || !Settings.HIDE_FEED_INTEREST.get()) return false;
            HookStatus.counted(FamilyNames.FEED_SUGGESTIONS, COUNT_HIDDEN);
            return true;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.FEED_SUGGESTIONS, "interest question", failure);
            return false;
        }
    }
}
