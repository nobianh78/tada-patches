/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.stories;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Sends the stories you've watched to the back of Home's story tray while "View stories
 * anonymously" and "Gray out stories you've watched" are both on (#241).
 *
 * <p>With both on, a watched story's ring turns gray on the phone ({@link StorySeenRings}), but
 * Instagram only moves a gray ring to the end of the tray when one of its own server flags says to.
 * Home's feed adapter asks the reel store for the tray's reels and the store, with that flag off,
 * hands them back in the order the server sent. So the patch hands the store's answer to
 * {@link #order}, which keeps the unseen reels first and the seen ones after, each half in the
 * order it came in. Nothing is added or dropped.
 *
 * <p>Either switch off, a pause or settings that aren't ready give the list back as it came, and so
 * does any failure deciding.
 */
public final class StoryTrayOrder {
    /** The hook's name in the diagnostics, and what it counts. */
    static final String HOOK = "story tray order";
    static final String MOVED = "watched stories moved to the back";

    /** Whether a reel in the store's list counts as seen, asked of Instagram's own check. */
    interface Seen {
        boolean of(Object reel);
    }

    private static final BooleanSupplier ON = StoryTrayOrder::grayedOut;

    private StoryTrayOrder() {
    }

    /** Whether the tray is ordered: views held back and watched stories grayed. */
    static boolean grayedOut() {
        return Utils.settingsReady() && StorySeen.anonymous() && Settings.GRAY_OUT_WATCHED_STORIES.get();
    }

    /**
     * Put by the patch on the list the reel [store] hands Home's feed adapter, just before it
     * returns. Returns [reels] itself unless the tray needs reordering. Never throws.
     */
    public static List<Object> order(List<Object> reels, Object store) {
        return order(reels, ON, reel -> seen(store, reel));
    }

    static List<Object> order(List<Object> reels, BooleanSupplier on, Seen seen) {
        try {
            if (reels == null || reels.size() < 2 || !on.getAsBoolean()) return reels;
            List<Object> unseen = new ArrayList<>(reels.size());
            List<Object> watched = new ArrayList<>();
            for (Object reel : reels) {
                if (reel != null && seen.of(reel)) watched.add(reel);
                else unseen.add(reel);
            }
            if (watched.isEmpty() || unseen.isEmpty()) return reels;
            unseen.addAll(watched);
            if (unseen.equals(reels)) return reels;
            HookStatus.counted(FamilyNames.STORY_SEEN, MOVED);
            Logger.printDebug(() -> "Story tray: moved " + watched.size() + " watched stories to the back");
            return unseen;
        } catch (Throwable failure) {
            try {
                HookStatus.threw(FamilyNames.STORY_SEEN, HOOK, failure);
            } catch (Throwable ignored) {
                // There is nowhere left to report a diagnostic failure.
            }
            return reels;
        }
    }

    // ------------------------------------------------------------------ what the patch fills in

    /** Filled in by the patch: Instagram's own check that [reel], one of [store]'s, has been seen by its account. */
    static boolean seen(Object store, Object reel) {
        return false;
    }
}
