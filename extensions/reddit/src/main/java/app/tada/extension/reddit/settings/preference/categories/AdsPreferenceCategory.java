/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.tada.extension.reddit.settings.preference.categories;

import static app.tada.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.tada.extension.reddit.patches.HideAdsPatch;
import app.tada.extension.reddit.settings.Settings;
import app.tada.extension.reddit.settings.preference.BooleanSettingPreference;

@SuppressWarnings("deprecation")
public class AdsPreferenceCategory extends ConditionalPreferenceCategory {
    public AdsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("tada_screen_ads_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return HideAdsPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_COMMENT_ADS
        ));
        addPreference(new BooleanSettingPreference(
                context,
                Settings.HIDE_POST_ADS
        ));
    }
}
