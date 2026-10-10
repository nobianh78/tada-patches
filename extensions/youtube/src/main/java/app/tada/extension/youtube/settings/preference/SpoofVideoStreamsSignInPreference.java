package app.tada.extension.youtube.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

import app.tada.extension.shared.settings.preference.OAuth2Preference;
import app.tada.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class SpoofVideoStreamsSignInPreference extends OAuth2Preference {

    public SpoofVideoStreamsSignInPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofVideoStreamsSignInPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofVideoStreamsSignInPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofVideoStreamsSignInPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean isSettingEnabled() {
        return Settings.SPOOF_VIDEO_STREAMS.get() &&
                Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get().supportsOAuth2;
    }
}
