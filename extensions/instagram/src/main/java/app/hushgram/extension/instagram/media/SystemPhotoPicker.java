/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * What the Use the system photo picker patch asks when you go to add a photo or video to a post.
 *
 * <p>Instagram already knows how to open an outside gallery: when it finds Google Photos on the
 * phone it builds an entry for it, a small object holding the intent that entry fires, its label and
 * its icon. A hidden flag decides whether that entry shows up in the gallery sheet. The patch puts
 * {@link #getPickerIntent} first thing in the factory that builds the entry, so when the switch is on
 * and the phone has a system photo picker, the entry fires Android's picker
 * ({@code ACTION_PICK_IMAGES}) instead of Google Photos. The system picker shows your own photos and
 * videos and any cloud gallery that plugs into it, and it hands Instagram only what you pick rather
 * than reaching across your whole library. The patch also routes the flag that shows the entry
 * through {@link #showEntry}, so the entry is there to tap even when Google Photos isn't installed.
 *
 * <p>Everything stays Instagram's own while the switch is off, HushGram is paused or the settings
 * aren't ready, when the phone has no system photo picker, or when anything in here fails: the
 * factory falls through to its Google Photos path and the flag keeps the value Instagram gave it.
 */
public final class SystemPhotoPicker {
    /** Android's system photo picker action, by name so this builds against any SDK (API 33+, or Play on older). */
    private static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

    private SystemPhotoPicker() {
    }

    /**
     * Asked first thing in the factory that builds Instagram's outside-gallery entry. Returns the
     * system photo picker's intent while the switch is on and the phone can open one, so the entry
     * fires that; null otherwise, so the factory builds its own Google Photos entry as before. Never
     * throws.
     */
    @Nullable
    public static Intent getPickerIntent() {
        try {
            HookStatus.invoked(FamilyNames.SYSTEM_PHOTO_PICKER);
            if (!Utils.settingsReady() || !Settings.SYSTEM_PHOTO_PICKER.get()) return null;
            Context context = Utils.getContext();
            if (context == null) return null;
            Intent intent = new Intent(ACTION_PICK_IMAGES);
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                Logger.printDebug(() -> "System photo picker: the phone has no system picker, leaving the entry as it was");
                return null;
            }
            Logger.printDebug(() -> "System photo picker: routing the gallery entry to the system picker");
            return intent;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.SYSTEM_PHOTO_PICKER, "picker intent", failure);
            return null;
        }
    }

    /**
     * Handed the hidden flag that decides whether the outside-gallery entry shows up. Returns true
     * while the switch is on, so the entry is there to tap even without Google Photos; otherwise the
     * flag as Instagram set it. Takes the flag as an int (non-zero is yes), since a register
     * Instagram's code types as int fails verification against a boolean parameter. Never throws.
     */
    public static boolean showEntry(int original) {
        try {
            if (!Utils.settingsReady() || !Settings.SYSTEM_PHOTO_PICKER.get()) return original != 0;
            return true;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.SYSTEM_PHOTO_PICKER, "entry visibility", failure);
            return original != 0;
        }
    }

    /**
     * The label stored on the entry the patch builds. Only reached once {@link #getPickerIntent} has
     * said the picker is on and available, so it needs no guard of its own. Never throws.
     */
    public static String pickerLabel() {
        return "Photos";
    }

    /**
     * The icon stored on the entry the patch builds: Android's own gallery glyph, or a blank drawable
     * if that can't be loaded. Only reached once the picker is on and available. Never throws.
     */
    public static Drawable pickerIcon() {
        try {
            Context context = Utils.getContext();
            if (context != null) {
                Drawable icon = context.getDrawable(android.R.drawable.ic_menu_gallery);
                if (icon != null) return icon;
            }
        } catch (Throwable failure) {
            Logger.printException(() -> "System photo picker: couldn't load the picker icon", failure);
        }
        return new ColorDrawable(0);
    }
}
