/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.reels;

import android.view.View;
import android.view.ViewGroup;
import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Helper for the "Swipe left to open the profile" patch.
 *
 * <p>Older Instagram builds opened the creator's profile when you swiped left on a reel. Now the
 * swipe goes to the Friends tab, or nowhere. Before it moves, the Reels viewer asks one check
 * whether a swipe from a reel to its side panel is allowed, and for an ordinary reel the last gate
 * of that check is a call that is usually false. The patch hands that call's answer to {@link #allow}
 * right after Instagram reads it. While the switch is on the answer is yes, and Instagram's own
 * panel picker then chooses the profile panel for an ordinary reel, the way it used to.
 *
 * <p>The panel itself is on only for accounts Instagram's server picks. The tab host turns it on
 * when you reach Reels if a second gate says yes, and the swipe container turns it off on each touch
 * if that gate says no. The patch hands the container's answer to {@link #allow} as well, calls
 * {@link #tabChanged} at the end of each tab change, which turns the panel on for the Reels tab, and
 * {@link #tabChanging} at the start of the next, which turns off only what HushGram turned on, so
 * Instagram picks the other tabs' panels as it always does.
 *
 * <p>With the panel on, the container still gives the swipe away. Before it starts its own drag it
 * walks the views under the finger and yields to any that can scroll sideways, and Instagram's walk
 * counts a pager whose input is off unless a server flag says otherwise. The main tabs' pager and the
 * Reels and Friends pager can always scroll. The patch hands that walk to {@link #childScrolls}:
 * on the Reels tab with the switch on, HushGram's walk passes over every sideways pager and the list
 * inside it, and turns a passed pager's input off so it can't start a drag of its own. The reels
 * themselves sit in an up-and-down pager, and that pager and everything inside it, a carousel
 * included, get Instagram's own walk. The patch also hands the container's own call turning the
 * tabs' pager input back on as a touch starts to {@link #pagerInput}, which keeps it off on Reels.
 * The next tab change gives every held pager its input back.
 *
 * <p>The hooks fail open: with the switch off, HushGram paused, the settings not read yet or
 * anything thrown, Instagram's own answer comes back and the panel stays as Instagram left it.
 * Touch and tab hooks run on the main thread.
 */
public final class SwipeToProfile {
    /** The step a failed switch read is reported under. */
    static final String SWITCH = "switch read";
    /** The step a failed panel turn is reported under. */
    static final String PANEL = "panel turn";
    /** The step a failed walk or pager hold is reported under. */
    static final String PAGER = "pager hold";

    /** What the tab and touch hooks need of Instagram, reached through the stubs the patch writes. */
    interface Panels {
        Object controller(Object host);

        Object container(Object controller);

        Object tab(Object host);

        Object reelsTab();

        Object panel(Object container);

        boolean panelOn(Object panel);

        void turn(Object container, Object panel, boolean on);

        boolean stockScrolls(View view, int dx, int x, int y, boolean checkView, boolean skipOff);

        boolean pager(Object view);

        boolean inputOn(Object pager);

        void input(Object pager, boolean on);
    }

    private static final Panels INSTAGRAM = new Panels() {
        @Override
        public Object controller(Object host) {
            return SwipeToProfileTargets.controller(host);
        }

        @Override
        public Object container(Object controller) {
            return SwipeToProfileTargets.container(controller);
        }

        @Override
        public Object tab(Object host) {
            return SwipeToProfileTargets.tab(host);
        }

        @Override
        public Object reelsTab() {
            return SwipeToProfileTargets.reelsTab();
        }

        @Override
        public Object panel(Object container) {
            return SwipeToProfileTargets.panel(container);
        }

        @Override
        public boolean panelOn(Object panel) {
            return SwipeToProfileTargets.panelOn(panel);
        }

        @Override
        public void turn(Object container, Object panel, boolean on) {
            SwipeToProfileTargets.turn(container, panel, on);
        }

        @Override
        public boolean stockScrolls(View view, int dx, int x, int y, boolean checkView, boolean skipOff) {
            return SwipeToProfileTargets.childScrolls(view, dx, x, y, checkView, skipOff);
        }

        @Override
        public boolean pager(Object view) {
            return SwipeToProfileTargets.pager(view);
        }

        @Override
        public boolean inputOn(Object pager) {
            return SwipeToProfileTargets.inputOn(pager);
        }

        @Override
        public void input(Object pager, boolean on) {
            SwipeToProfileTargets.input(pager, on);
        }
    };

    private static volatile boolean logged;
    private static volatile boolean loggedPanel;
    private static volatile boolean loggedPager;
    private static volatile String lastClaim;

    /** Whether the last tab change landed on Reels. */
    private static volatile boolean onReels;

    /** The panel HushGram turned on and its container, until the next tab change turns it off. */
    private static WeakReference<Object> turnedPanel;
    private static WeakReference<Object> turnedContainer;

    /** The pagers HushGram turned the input off on, until the next tab change turns it back on. */
    private static final List<WeakReference<Object>> heldPagers = new ArrayList<>();

    private SwipeToProfile() {
    }

    /**
     * Called with Instagram's answer to whether this swipe may go on to the side panel, non-zero for
     * yes. Returns true while the switch is on, otherwise the answer as it came. Never throws.
     */
    public static boolean allow(int stock) {
        return allow(stock != 0, SwipeToProfile::switchedOn);
    }

    static boolean allow(boolean stock, BooleanSupplier on) {
        try {
            HookStatus.invoked(FamilyNames.SWIPE_TO_PROFILE);
            if (stock) return true;
            boolean open = on.getAsBoolean();
            if (open && !logged) {
                logged = true;
                Logger.printDebug(() -> "SwipeToProfile: let a swipe on a reel go to its profile");
            }
            return open;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, SWITCH, t);
            return stock;
        }
    }

    /**
     * Called first thing in each tab change, before Instagram picks the next tab's panels. Turns the
     * profile panel off if HushGram turned it on and it's still on. Never throws.
     */
    public static void tabChanging(Object host) {
        tabChanging(INSTAGRAM);
    }

    static void tabChanging(Panels panels) {
        onReels = false;
        release(panels);
        try {
            Object panel = turnedPanel == null ? null : turnedPanel.get();
            Object container = turnedContainer == null ? null : turnedContainer.get();
            turnedPanel = null;
            turnedContainer = null;
            if (panel != null && container != null && panels.panelOn(panel)) panels.turn(container, panel, false);
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, PANEL, t);
        }
    }

    /**
     * Called at each return of a tab change. Notes whether the tab is Reels, and there with the switch
     * on, turns the profile panel on when Instagram left it off, and remembers it. Never throws.
     */
    public static void tabChanged(Object host) {
        tabChanged(host, INSTAGRAM, SwipeToProfile::switchedOn);
    }

    static void tabChanged(Object host, Panels panels, BooleanSupplier on) {
        try {
            HookStatus.invoked(FamilyNames.SWIPE_TO_PROFILE);
            Object tab = panels.tab(host);
            onReels = tab != null && tab == panels.reelsTab();
            if (!onReels || !on.getAsBoolean()) return;
            Object controller = panels.controller(host);
            Object container = controller == null ? null : panels.container(controller);
            Object panel = container == null ? null : panels.panel(container);
            if (panel == null || panels.panelOn(panel)) return;
            panels.turn(container, panel, true);
            turnedPanel = new WeakReference<>(panel);
            turnedContainer = new WeakReference<>(container);
            if (!loggedPanel) {
                loggedPanel = true;
                Logger.printDebug(() -> "SwipeToProfile: turned the Reels profile panel on");
            }
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, PANEL, t);
        }
    }

    /**
     * Called in place of the swipe container's walk of the views under the finger, which answers
     * whether one of them, or {@code view} itself when {@code checkView}, can scroll sideways against
     * {@code dx}, with (x, y) in {@code view}'s coordinates. On Reels with the switch on, HushGram
     * walks them the same way but passes over every sideways pager and the list inside it, and holds
     * a passed pager that still takes swipes. An up-and-down pager and what's inside it get
     * Instagram's walk. Otherwise Instagram walks them all, and a pager HushGram held gets its input
     * back. Never throws on HushGram's account. {@code checkView} and {@code skipOff} come as Instagram's
     * registers hold them, non-zero for yes.
     */
    public static boolean childScrolls(View view, int dx, int x, int y, int checkView, int skipOff) {
        return childScrolls(view, dx, x, y, checkView != 0, skipOff != 0, INSTAGRAM, SwipeToProfile::reelsOpen);
    }

    static boolean childScrolls(View view, int dx, int x, int y, boolean checkView, boolean skipOff, Panels panels, BooleanSupplier on) {
        boolean open = false;
        try {
            HookStatus.invoked(FamilyNames.SWIPE_TO_PROFILE);
            open = on.getAsBoolean();
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, SWITCH, t);
        }
        if (open) {
            try {
                return scrolls(view, dx, x, y, checkView, skipOff, panels);
            } catch (Throwable t) {
                HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, PAGER, t);
                release(panels);
            }
        } else {
            release(panels);
        }
        return panels.stockScrolls(view, dx, x, y, checkView, skipOff);
    }

    /**
     * Instagram's walk as 452 has it, children first, last child first, except that a sideways pager
     * and the list inside it never count as scrolling. An up-and-down pager, the reels' own, goes to
     * Instagram's walk whole, so a carousel in a reel still takes its swipe.
     */
    private static boolean scrolls(View view, int dx, int x, int y, boolean checkView, boolean skipOff, Panels panels) {
        boolean pager = panels.pager(view);
        if (pager && !view.canScrollHorizontally(1) && !view.canScrollHorizontally(-1)) {
            return panels.stockScrolls(view, dx, x, y, checkView, skipOff);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int left = x + group.getScrollX();
            int top = y + group.getScrollY();
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                float childX = child.getX();
                float childY = child.getY();
                if (left < childX || left >= childX + child.getWidth() || top < childY || top >= childY + child.getHeight()) continue;
                if (scrolls(child, dx, left - (int) childX, top - (int) childY, true, skipOff, panels)) return true;
            }
        }
        if (pager) {
            hold(view, panels);
            return false;
        }
        if (panels.pager(view.getParent())) return false;
        if (!checkView || !view.canScrollHorizontally(-dx)) return false;
        String claim = view.getClass().getName();
        if (!claim.equals(lastClaim)) {
            lastClaim = claim;
            Logger.printDebug(() -> "SwipeToProfile: a " + claim + " under the finger takes the swipe");
        }
        return true;
    }

    /**
     * Called in place of the swipe container's own call turning the tabs' pager input on or off as a
     * touch starts. On Reels with the switch on, input Instagram turns on stays off until the next tab
     * change. Otherwise the pager gets what Instagram asked for. The call to the pager is Instagram's
     * own, so it fails the way Instagram's would; nothing else here throws. {@code wanted} is non-zero
     * for on.
     */
    public static void pagerInput(Object pager, int wanted) {
        pagerInput(pager, wanted != 0, INSTAGRAM, SwipeToProfile::reelsOpen);
    }

    static void pagerInput(Object pager, boolean wanted, Panels panels, BooleanSupplier on) {
        if (pager == null) return;
        boolean hold = false;
        try {
            HookStatus.invoked(FamilyNames.SWIPE_TO_PROFILE);
            hold = wanted && on.getAsBoolean();
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, SWITCH, t);
        }
        if (hold) remember(pager);
        else forget(pager);
        panels.input(pager, wanted && !hold);
    }

    /** Turns a pager's input off when it's on, and remembers it to turn back on. */
    private static void hold(Object pager, Panels panels) {
        if (!panels.inputOn(pager)) return;
        panels.input(pager, false);
        remember(pager);
    }

    private static void remember(Object pager) {
        for (int i = heldPagers.size() - 1; i >= 0; i--) {
            Object held = heldPagers.get(i).get();
            if (held == pager) return;
            if (held == null) heldPagers.remove(i);
        }
        heldPagers.add(new WeakReference<>(pager));
        if (!loggedPager) {
            loggedPager = true;
            Logger.printDebug(() -> "SwipeToProfile: held a pager so the swipe reaches the profile panel");
        }
    }

    private static void forget(Object pager) {
        for (int i = heldPagers.size() - 1; i >= 0; i--) {
            Object held = heldPagers.get(i).get();
            if (held == null || held == pager) heldPagers.remove(i);
        }
    }

    /** Gives each pager HushGram held its input back. */
    private static void release(Panels panels) {
        if (heldPagers.isEmpty()) return;
        List<WeakReference<Object>> held = new ArrayList<>(heldPagers);
        heldPagers.clear();
        for (WeakReference<Object> reference : held) {
            Object pager = reference.get();
            if (pager == null) continue;
            try {
                panels.input(pager, true);
            } catch (Throwable t) {
                HookStatus.threw(FamilyNames.SWIPE_TO_PROFILE, PAGER, t);
            }
        }
    }

    private static boolean switchedOn() {
        return Utils.settingsReady() && Settings.REEL_SWIPE_TO_PROFILE.get();
    }

    private static boolean reelsOpen() {
        return reelsOpen(SwipeToProfile::switchedOn);
    }

    /** Whether the last tab change landed on Reels and the switch is on. */
    static boolean reelsOpen(BooleanSupplier on) {
        return onReels && on.getAsBoolean();
    }
}
