/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.hushgram.extension.shared.settings.BaseSettings;
import app.hushgram.extension.shared.settings.BooleanSetting;
import app.hushgram.extension.shared.settings.Setting;
import app.hushgram.extension.shared.settings.preference.QuickSetups;

/**
 * Which switches each Quick setups choice changes, all in one place.
 *
 * <p>Quiet and Privacy only turn switches on, and only switches whose effect is plain from their
 * name and takes nothing away that someone would miss. Left out on purpose: viewing stories, lives
 * and messages' media without being seen, Spoof location and Keep in chat, which change what other
 * people see of you and are each a choice to make on its own; Don't send reel watch history and
 * Don't save recent searches, which change what Instagram shows you next; Open links in your
 * browser, which changes how every link opens; Hide shopping, Hide Meta AI in search and the
 * story rewinds, recaps, paid partnership reels, Explore grid, notes and Instants switches, which
 * hide things people use; and the locks. Back to defaults puts every switch a settings file
 * carries back the way it started, except the two locks, Debug logging and the category view.
 */
final class QuickSetupKeys {
    /** Fewer ads, suggestions, nags and promotions. */
    static final List<BooleanSetting> QUIET = Collections.unmodifiableList(Arrays.asList(
            Settings.HIDE_ADS,
            Settings.REMOVE_BUILD_EXPIRED_POPUP,
            Settings.HIDE_SUGGESTED_ACCOUNTS,
            Settings.HIDE_SUGGESTED_POSTS,
            Settings.HIDE_THREADS_POSTS,
            Settings.HIDE_FEED_SURVEYS,
            Settings.HIDE_FEED_INTEREST,
            Settings.HIDE_FEED_REELS,
            Settings.HIDE_SUGGESTED_STORIES,
            Settings.HIDE_STORY_SUGGESTIONS,
            Settings.HIDE_MUSIC_CARD,
            Settings.HIDE_ADD_YOURS_CARDS,
            Settings.HIDE_META_AI_POSTS,
            Settings.HIDE_META_AI_PROFILE_BANNER,
            Settings.HIDE_REEL_CHIPS,
            Settings.HIDE_REELS_SUGGESTIONS,
            Settings.HIDE_SEARCH_SUGGESTIONS,
            Settings.HIDE_INBOX_SUGGESTIONS,
            Settings.HIDE_PROFILE_SUGGESTIONS,
            Settings.HIDE_FOLLOW_LIST_SUGGESTIONS,
            Settings.HIDE_PROFILE_COMPLETION,
            Settings.HIDE_FOLLOW_BACK_SUGGESTIONS,
            Settings.HIDE_NOTIFICATION_SUGGESTIONS,
            Settings.HIDE_NOTIFICATION_FEATURED));

    /** Less said about you: seen receipts, typing, active status, screenshots, link tags and usage reports. */
    static final List<BooleanSetting> PRIVACY = Collections.unmodifiableList(Arrays.asList(
            Settings.READ_WITHOUT_SEEN_RECEIPT,
            Settings.HIDE_TYPING,
            Settings.HIDE_ACTIVE_STATUS,
            Settings.HIDE_SCREENSHOTS,
            Settings.ALLOW_SCREENSHOTS,
            Settings.SANITIZE_SHARING_LINKS,
            Settings.STOP_UPLOAD_ALARM));

    /** Switches Back to defaults leaves as they are, though a settings file carries them. */
    static final List<BooleanSetting> KEPT = Collections.unmodifiableList(Arrays.asList(
            Settings.LOCK_MESSAGES, Settings.LOCK_APP, BaseSettings.DEBUG, Settings.CATEGORY_PAGES));

    private QuickSetupKeys() {
    }

    static Map<BooleanSetting, Boolean> quiet() {
        return QuickSetups.allOn(QUIET);
    }

    static Map<BooleanSetting, Boolean> privacy() {
        return QuickSetups.allOn(PRIVACY);
    }

    /** Every switch a settings file carries in this build but the {@link #KEPT} ones, the way it started. */
    static Map<BooleanSetting, Boolean> defaults() {
        return QuickSetups.defaults(defaultSwitches());
    }

    /**
     * The switches of {@link ConfigurationBackup#eligible()}, the same list a settings file
     * carries, without the {@link #KEPT} ones. Pause isn't among them, so it stays as it is.
     */
    static List<BooleanSetting> defaultSwitches() {
        List<BooleanSetting> switches = new ArrayList<>();
        for (Setting<?> setting : ConfigurationBackup.eligible().values()) {
            if (setting instanceof BooleanSetting && !KEPT.contains(setting)) switches.add((BooleanSetting) setting);
        }
        return Collections.unmodifiableList(switches);
    }
}
