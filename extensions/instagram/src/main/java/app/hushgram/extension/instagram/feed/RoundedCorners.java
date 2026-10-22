/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.feed;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Helper for the "Rounded post corners" patch.
 *
 * <p>Instagram's feed makes one touch holder for each media frame of a post: the frame a photo, a video
 * preview or one page of a carousel sits in. The patch hands each new holder to {@link #made}, which
 * reads the holder's frame and, while the choice is on, gives that frame a rounded outline and turns on
 * clipping to it. That is one outline provider and one flag on a view that was just made, and the
 * view system cuts the picture and the video (a TextureView is drawn inside its parent, so it is cut
 * too) at draw time, so scrolling does no work of its own. A frame already rounded to the chosen size
 * is not touched again. Stories, the profile grid, Explore and the Reels tab don't use these holders and
 * stay as Instagram draws them.
 *
 * <p>The outline provider Instagram had and its clipping flag are kept for each frame touched. With
 * the choice off or HushGram paused, the next holder made puts every kept frame back as it was and
 * forgets them, and until then nothing runs. Frames are held weakly. The hook fails open: anything
 * thrown leaves the frame as Instagram made it.
 */
public final class RoundedCorners {
    /** The step a failure is reported under. */
    static final String MADE = "post frame";

    /** What {@link #made} counts for each frame it rounds. */
    static final String ROUNDED = "frames rounded";

    /** What a frame was before it was rounded, and the radius it has now. */
    private static final class Kept {
        final ViewOutlineProvider provider;
        final boolean clipped;
        float radius;

        Kept(View view) {
            provider = view.getOutlineProvider();
            clipped = view.getClipToOutline();
        }
    }

    /** Cuts a view's outline to a rounded rectangle of its size. The view system asks again whenever the size changes. */
    private static final class Rounded extends ViewOutlineProvider {
        final float radius;

        Rounded(float radius) {
            this.radius = radius;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }

    /** The frames rounded now, with what they were. Main thread only. */
    private static final Map<View, Kept> kept = new WeakHashMap<>();

    /** Reads the frame a holder keeps. Replaced by tests. */
    static Function<Object, Object> frames = RoundedTargets::frame;

    private static volatile boolean logged;

    private RoundedCorners() {
    }

    /**
     * Called last thing in the constructor of a feed post's touch holder, with the new [holder]. Never throws.
     */
    public static void made(Object holder) {
        try {
            if (!Utils.settingsReady() || !Settings.ROUNDED_POST_CORNERS.get()) {
                if (!kept.isEmpty()) putBack();
                return;
            }
            HookStatus.invoked(FamilyNames.ROUNDED_POST_CORNERS);
            Object frame = frames.apply(holder);
            if (!(frame instanceof View)) return;
            View view = (View) frame;
            float radius = Settings.ROUNDED_CORNER_SIZE.get().dp * view.getResources().getDisplayMetrics().density;
            Kept before = kept.get(view);
            if (before == null) {
                before = new Kept(view);
                kept.put(view, before);
            } else if (before.radius == radius) {
                return;
            }
            before.radius = radius;
            view.setOutlineProvider(new Rounded(radius));
            view.setClipToOutline(true);
            HookStatus.counted(FamilyNames.ROUNDED_POST_CORNERS, ROUNDED);
            if (!logged) {
                logged = true;
                Logger.printDebug(() -> "Rounded post corners: rounded a post's frame");
            }
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.ROUNDED_POST_CORNERS, MADE, failure);
        }
    }

    /** Gives every frame rounded so far Instagram's outline provider and clipping back, and forgets them. */
    private static void putBack() {
        for (Map.Entry<View, Kept> entry : new ArrayList<>(kept.entrySet())) {
            View view = entry.getKey();
            if (view == null) continue;
            view.setOutlineProvider(entry.getValue().provider);
            view.setClipToOutline(entry.getValue().clipped);
        }
        kept.clear();
    }

    /** How many frames are rounded now. Tests only. */
    static int roundedForTests() {
        return kept.size();
    }

    /** Forgets what was logged and kept. Tests only. */
    static void forgetForTests() {
        logged = false;
        kept.clear();
        frames = RoundedTargets::frame;
    }
}
