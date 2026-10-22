/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.feed;

import android.content.Context;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.instagram.settings.SettingsStatus;

/**
 * Helper for the Hide Follow on feed posts patch (#248): keeps the Follow and Subscribe buttons
 * beside a feed post's author out of the post header.
 *
 * <p>Instagram reuses these ids in other places (a story, a reel, a profile), so {@link ViewHider}
 * counts them only inside the feed post header, and never touches the view stubs the header
 * inflates them from. The author's name, the time and the menu are not named and stay.
 */
public final class FeedFollowButton {
    /** The header of a feed post, which holds the author's name and the buttons beside it. */
    static final String HEADER = "row_feed_profile_header";
    /** The buttons: the one beside the name, the one in the header, and Subscribe. */
    static final String INLINE_FOLLOW = "inline_follow_button";
    static final String HEADER_FOLLOW = "follow_in_header";
    static final String SUBSCRIBE = "subscribe_button";

    static final String HOOK = "feed follow button";
    static final String COUNT_HIDDEN = "feed follow buttons hidden";

    private static final ViewHider HIDER = new ViewHider(FamilyNames.FEED_FOLLOW, HOOK, COUNT_HIDDEN,
            () -> Settings.HIDE_FEED_FOLLOW.get(), HEADER, INLINE_FOLLOW, HEADER_FOLLOW, SUBSCRIBE);

    private FeedFollowButton() {
    }

    /** Called as the application starts. Does nothing in a build without the patch. */
    public static void install(Context context) {
        HIDER.install(context, SettingsStatus.feedFollow());
    }

    static ViewHider hider() {
        return HIDER;
    }
}
