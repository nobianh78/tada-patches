/*
 * Copyright 2026 HushPinterest contributors
 * Taken from HushPinterest's
 * extensions/shared/library/src/main/java/app/hushpinterest/extension/shared/settings/preference/QuickSetups.java
 * (https://github.com/SysAdminDoc/HushPinterest, commit 44fb93384a319d8e5d324f51b416efdc3d3147e6).
 * Modified for HushGram (Instagram), 2026.
 *
 * Built on SysAdminDoc/hushfeed (GPL-3.0).
 */
package app.hushgram.extension.shared.settings.preference;

import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.TwoStatePreference;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.hushgram.extension.shared.settings.BooleanSetting;

/**
 * Quick setups: a set of switches flipped in one tap. Each choice is a map from switch to the
 * value it puts there; which switches go in which choice is the app's call, in its own class.
 *
 * <p>Every change goes through the switch's row, the same as a tap on it: the row's own change
 * listener is asked first and can say no, then the row is set, so the page's listener saves it,
 * keeps the rows that depend on it in step and notes a restart when the switch needs one. A
 * switch whose patch isn't in this build has no row and is left alone.
 */
@SuppressWarnings("deprecation")
public final class QuickSetups {
    /** Finds a switch's row by its key, on the screen or kept off it. */
    public interface Rows {
        @Nullable
        Preference find(String key);
    }

    private QuickSetups() {
    }

    /** Each of [settings] turned on. */
    public static Map<BooleanSetting, Boolean> allOn(List<BooleanSetting> settings) {
        Map<BooleanSetting, Boolean> values = new LinkedHashMap<>();
        for (BooleanSetting setting : settings) values.put(setting, Boolean.TRUE);
        return Collections.unmodifiableMap(values);
    }

    /** Each of [settings] back to the value it started with. */
    public static Map<BooleanSetting, Boolean> defaults(List<BooleanSetting> settings) {
        Map<BooleanSetting, Boolean> values = new LinkedHashMap<>();
        for (BooleanSetting setting : settings) values.put(setting, setting.defaultValue);
        return Collections.unmodifiableMap(values);
    }

    /** {@link #apply(Rows, Map)} for the rows [screen] holds. */
    public static int apply(PreferenceGroup screen, Map<BooleanSetting, Boolean> values) {
        return apply(screen::findPreference, values);
    }

    /**
     * Sets each switch row [rows] finds for a key in [values] that shows something else, and
     * answers how many changed. A row that already shows its value, or whose listener refuses the
     * change, doesn't count.
     */
    public static int apply(Rows rows, Map<BooleanSetting, Boolean> values) {
        int changed = 0;
        for (Map.Entry<BooleanSetting, Boolean> value : values.entrySet()) {
            Preference row = rows.find(value.getKey().key);
            if (!(row instanceof TwoStatePreference) || value.getValue() == null) continue;
            TwoStatePreference toggle = (TwoStatePreference) row;
            boolean wanted = value.getValue();
            if (toggle.isChecked() == wanted) continue;
            // What Preference.callChangeListener does; it's protected, so ask the listener here.
            Preference.OnPreferenceChangeListener listener = toggle.getOnPreferenceChangeListener();
            if (listener != null && !listener.onPreferenceChange(toggle, wanted)) continue;
            toggle.setChecked(wanted);
            changed++;
        }
        return changed;
    }
}
