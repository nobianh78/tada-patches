/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.morphe.patches.instagram.feed.feedfollow

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.instagram.misc.extension.enableStatus
import app.morphe.patches.instagram.misc.extension.instagramExtensionPatch
import app.morphe.patches.instagram.misc.extension.requireStatusMethod
import app.morphe.patches.instagram.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities

internal const val FEED_FOLLOW_STATUS = "feedFollow"

/**
 * Takes the Follow and Subscribe buttons off the top of feed posts (#248). See FeedFollowButton in
 * the extension.
 *
 * It changes no Instagram code. The extension sets the buttons gone before a frame shows them, found
 * by the ids Instagram's own layout gives them and only inside a feed post's header, so the same
 * ids in a story, a reel or a profile stay. The application-start hook the settings patch already
 * writes is what starts the watching, so this patch only switches the status method on, and the
 * settings screen then offers the switch. In the default selection with its switch off.
 */
@Suppress("unused")
val hideFeedFollowPatch = bytecodePatch(
    name = "Hide Follow on feed posts",
    description = "Takes the Follow and Subscribe buttons off the top of posts in your feed. The name, time and menu " +
        "stay. Starts off. Turn it on in TADa settings > Feed.",
) {
    category("Interface")
    dependsOn(settingsPatch, instagramExtensionPatch)
    compatibleWith(*AppCompatibilities.instagram())

    execute {
        requireStatusMethod(FEED_FOLLOW_STATUS)
        enableStatus(FEED_FOLLOW_STATUS)
    }
}
