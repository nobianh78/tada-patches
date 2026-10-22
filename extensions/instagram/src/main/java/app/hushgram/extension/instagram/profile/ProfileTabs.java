/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.profile;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;
import app.hushgram.extension.shared.settings.HushgramPause;

/**
 * Helper for the "Hide profile tabs" patch (#254): leaves the Reels, Reposts and Tagged tabs off
 * profiles, yours and other people's, and keeps the grid.
 *
 * <p>Instagram names each profile tab with a string key ({@code profile_media_grid},
 * {@code profile_clips}, {@code profile_reposts}, {@code profile_tagged_media_photos_of_you}) and
 * builds the list of keys for the profile it's showing in one method, which the tab strip, the pager
 * and the tab providers are all made from. The patch hands that list to {@link #tabs} just before the
 * method returns. A tab is dropped by its key, never by its place in the list, and a key Instagram
 * didn't put in the list is never added. With one tab left Instagram draws no strip at all, as it
 * already does for an account with nothing but a grid.
 *
 * <p>Fails open: with nothing saved, HushGram paused, the settings not read yet or anything thrown,
 * the list goes back as Instagram made it.
 */
public final class ProfileTabs {
    /** The grid of posts. Never offered, never dropped. */
    static final String GRID = "profile_media_grid";

    /** The tabs the setting can leave off, as Instagram's keys, in the order the tab strip shows them. */
    public static final List<String> OFFERED = Collections.unmodifiableList(Arrays.asList(
            "profile_clips", "profile_reposts", "profile_tagged_media_photos_of_you"));

    /** What's counted for each profile whose tab list lost a tab. */
    static final String LEFT_OFF = "Profile tabs left off";

    private ProfileTabs() {
    }

    /**
     * Injected where Instagram's builder of a profile's tab keys returns its list. Answers a copy of
     * [tabs] without the tabs the setting names, or [tabs] itself when there's nothing to drop.
     * Never throws, and never waits for the settings.
     */
    public static List<String> tabs(List<String> tabs) {
        try {
            HookStatus.invoked(FamilyNames.PROFILE_TABS);
            if (tabs == null || !Utils.settingsReady() || HushgramPause.isPaused()) return tabs;
            Set<String> hidden = parse(Settings.HIDDEN_PROFILE_TABS.get());
            List<String> kept = without(tabs, hidden);
            if (kept == null) return tabs;
            HookStatus.counted(FamilyNames.PROFILE_TABS, LEFT_OFF);
            return kept;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.PROFILE_TABS, "profile tabs", failure);
            return tabs;
        }
    }

    /**
     * A copy of [tabs] without the keys in [hidden], or null when nothing would change or the
     * result would have no grid left (Instagram's list always starts with it, so a list without it
     * isn't one this knows how to thin).
     */
    @Nullable
    static List<String> without(List<String> tabs, Set<String> hidden) {
        if (hidden.isEmpty() || !tabs.contains(GRID)) return null;
        List<String> kept = new ArrayList<>(tabs.size());
        for (String tab : tabs) {
            if (tab == null || !hidden.contains(tab)) kept.add(tab);
        }
        return kept.size() == tabs.size() ? null : kept;
    }

    /** The offered keys in [saved], without blanks, repeats or anything this doesn't offer. */
    public static Set<String> parse(@Nullable String saved) {
        Set<String> keys = new LinkedHashSet<>();
        if (saved == null) return keys;
        for (String part : saved.split(",")) {
            String key = part.trim();
            if (OFFERED.contains(key)) keys.add(key);
        }
        return keys;
    }

    /** [hidden] as the saved value, in the order the tabs are offered. */
    public static String join(Set<String> hidden) {
        List<String> keys = new ArrayList<>();
        for (String key : OFFERED) {
            if (hidden.contains(key)) keys.add(key);
        }
        return String.join(",", keys);
    }
}
