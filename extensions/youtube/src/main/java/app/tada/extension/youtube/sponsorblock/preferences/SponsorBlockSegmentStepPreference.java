/*
 * Copyright 2026 TADa.
 * https://github.com/TADaApp/tada-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to TADa contributions.
 */

package app.tada.extension.youtube.sponsorblock.preferences;

import static app.tada.extension.shared.StringRef.str;

import android.content.Context;
import android.util.AttributeSet;

import app.tada.extension.shared.Logger;
import app.tada.extension.shared.Utils;
import app.tada.extension.shared.settings.preference.ResettableEditTextPreference;

/**
 * Numeric input for the "create new segment" time-adjustment step. Rejects zero and noninteger
 * values with a toast, matching the legacy SponsorBlock preferences behavior.
 */
@SuppressWarnings({"unused", "deprecation"})
public class SponsorBlockSegmentStepPreference extends ResettableEditTextPreference {

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        installValidator();
    }

    public SponsorBlockSegmentStepPreference(Context context) {
        super(context);
        installValidator();
    }

    private void installValidator() {
        setOnPreferenceChangeListener((p, newValue) -> {
            try {
                if (Integer.parseInt(newValue.toString()) != 0) {
                    return true;
                }
            } catch (NumberFormatException ex) {
                Logger.printInfo(() -> "Invalid new segment step", ex);
            }
            Utils.showToastLong(str("tada_sb_create_new_segment_step_invalid"));
            return false;
        });
    }
}
