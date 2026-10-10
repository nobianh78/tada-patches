package app.tada.extension.shared.settings.preference;

import static app.tada.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.tada.extension.shared.Logger;
import app.tada.extension.shared.Utils;
import app.tada.extension.shared.settings.BaseSettings;

/**
 * A custom preference that clears the TADa debug log buffer when clicked.
 * Invokes the {@link Logger#clearLogBufferData()} method.
 */
@SuppressWarnings({"unused", "deprecation"})
public class ClearLogBufferPreference extends Preference {

    {
        setOnPreferenceClickListener(pref -> {
            clearLogBuffer();
            return true;
        });
    }

    public ClearLogBufferPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public ClearLogBufferPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ClearLogBufferPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ClearLogBufferPreference(Context context) {
        super(context);
    }

    /**
     * Clears the internal log buffer and displays a toast with the result.
     */
    public static void clearLogBuffer() {
        if (!BaseSettings.DEBUG.get()) {
            Utils.showToastShort(str("tada_debug_logs_disabled"));
            return;
        }

        // Show toast before clearing, otherwise toast log will still remain.
        Utils.showToastShort(str("tada_debug_logs_clear_toast"));
        Logger.clearLogBufferData();
    }
}
