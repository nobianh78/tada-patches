/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.feed;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import app.hushgram.extension.shared.ResourceIdCache;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Keeps a few of Instagram's views gone while a screen shows them, for the patches that change no
 * Instagram code (the comment emoji row, the Follow button beside a feed post's author).
 *
 * <p>It watches the activities once the application is up. While the switch is on, an activity that
 * is resumed gets one global-layout listener, which only notes that the screen was laid out again,
 * and one pre-draw listener. Before a frame the pre-draw listener puts back to GONE anything it hid
 * that Instagram showed again, and after a layout it looks through the window once for the views
 * named. A hit is set GONE and the frame is drawn again, so it never shows and the row closes up
 * around it. A view stub is never touched: only the view it inflated, which has its own id.
 *
 * <p>With a [scope] the views count only inside a view of that id, so an id Instagram reuses
 * elsewhere (a Follow button in a story or a reel) stays. With the switch off, HushGram paused or
 * the settings not read, an activity gets no listener, and one already listening drops both at its
 * next frame. A resumed activity decides again, so the switch needs no restart. Views hidden stay
 * gone until Instagram binds them again, which it does itself when a row comes back on screen.
 * Anything thrown leaves the screen as Instagram drew it.
 */
final class ViewHider {
    /** The most views looked at in one search, so a strange tree can't make a long one. */
    static final int MAX_VIEWS = 6000;

    private final String family;
    private final String hook;
    private final String counted;
    private final BooleanSupplier on;
    @Nullable
    private final String scope;
    private final String[] names;
    private final ResourceIdCache ids = new ResourceIdCache();
    private final WeakHashMap<Activity, Session> sessions = new WeakHashMap<>();
    private volatile boolean registered;

    ViewHider(String family, String hook, String counted, BooleanSupplier on, @Nullable String scope, String... names) {
        this.family = family;
        this.hook = hook;
        this.counted = counted;
        this.on = on;
        this.scope = scope;
        this.names = names;
    }

    /** Called as the application starts. Does nothing in a build without the patch. */
    void install(Context context, boolean built) {
        try {
            if (registered || !built || !(context instanceof Application)) return;
            registered = true;
            ((Application) context).registerActivityLifecycleCallbacks(new Watcher());
        } catch (Throwable failure) {
            HookStatus.threw(family, hook + " start", failure);
        }
    }

    /** Whether anything is hidden now: the switch on, HushGram not paused, the settings read. */
    boolean wanted() {
        return Utils.settingsReady() && on.getAsBoolean();
    }

    /** Starts watching [activity] if the switch is on. Called as an activity resumes. */
    void watch(Activity activity) {
        try {
            if (!wanted()) return;
            Session session;
            synchronized (sessions) {
                session = sessions.get(activity);
                if (session == null) {
                    session = new Session(activity);
                    sessions.put(activity, session);
                }
            }
            session.attach();
        } catch (Throwable failure) {
            HookStatus.threw(family, hook, failure);
        }
    }

    void unwatch(Activity activity) {
        try {
            Session session;
            synchronized (sessions) {
                session = sessions.remove(activity);
            }
            if (session != null) session.detach();
        } catch (Throwable failure) {
            HookStatus.threw(family, hook, failure);
        }
    }

    /** Tests give a view an id of their own, since a test app has none of Instagram's. */
    void idForTests(Context context, String name, int id) {
        ids.putForTests(context.getPackageName(), name, id);
    }

    void resetForTests() {
        ids.clear();
        synchronized (sessions) {
            for (Session session : new ArrayList<>(sessions.values())) session.detach();
            sessions.clear();
        }
    }

    int sessionsForTests() {
        synchronized (sessions) {
            return sessions.size();
        }
    }

    boolean listeningForTests(Activity activity) {
        synchronized (sessions) {
            Session session = sessions.get(activity);
            return session != null && session.observer != null;
        }
    }

    private int id(Activity activity, String name) {
        return ids.resolve(activity.getResources(), activity.getPackageName(), name, false);
    }

    /** One activity's listeners and the views hidden in its window. */
    private final class Session implements ViewTreeObserver.OnGlobalLayoutListener,
            ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
        private final WeakReference<Activity> activity;
        private final List<WeakReference<View>> hidden = new ArrayList<>();
        @Nullable
        private ViewTreeObserver observer;
        private boolean waiting;
        private boolean dirty = true;

        Session(Activity activity) {
            this.activity = new WeakReference<>(activity);
        }

        void attach() {
            Activity owner = activity.get();
            if (owner == null || observer != null || waiting) return;
            HookStatus.invoked(family);
            View decor = owner.getWindow().getDecorView();
            if (decor.isAttachedToWindow()) {
                listen(decor);
            } else {
                // An observer taken from a view that isn't attached is a stand-in thrown away on attach.
                waiting = true;
                decor.addOnAttachStateChangeListener(this);
            }
        }

        private void listen(View decor) {
            ViewTreeObserver tree = decor.getViewTreeObserver();
            tree.addOnGlobalLayoutListener(this);
            tree.addOnPreDrawListener(this);
            observer = tree;
            dirty = true;
            decor.invalidate();
        }

        void detach() {
            ViewTreeObserver tree = observer;
            observer = null;
            if (tree != null && tree.isAlive()) {
                tree.removeOnGlobalLayoutListener(this);
                tree.removeOnPreDrawListener(this);
            }
            Activity owner = activity.get();
            if (waiting && owner != null) owner.getWindow().getDecorView().removeOnAttachStateChangeListener(this);
            waiting = false;
            hidden.clear();
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            if (!waiting) return;
            waiting = false;
            view.removeOnAttachStateChangeListener(this);
            if (observer == null && wanted()) listen(view);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
        }

        @Override
        public void onGlobalLayout() {
            dirty = true;
        }

        @Override
        public boolean onPreDraw() {
            try {
                Activity owner = activity.get();
                if (owner == null || !wanted()) {
                    detach();
                    return true;
                }
                boolean changed = false;
                for (Iterator<WeakReference<View>> each = hidden.iterator(); each.hasNext(); ) {
                    View view = each.next().get();
                    if (view == null) {
                        each.remove();
                    } else if (view.getVisibility() != View.GONE) {
                        view.setVisibility(View.GONE);
                        changed = true;
                    }
                }
                if (dirty) {
                    dirty = false;
                    changed |= search(owner);
                }
                // The frame is drawn again once the row has closed up, so the view never shows.
                return !changed;
            } catch (Throwable failure) {
                HookStatus.threw(family, hook, failure);
                return true;
            }
        }

        private boolean search(Activity owner) {
            Set<Integer> wanted = new HashSet<>();
            for (String name : names) {
                int id = id(owner, name);
                if (id == 0) {
                    HookStatus.missingViewId(family, name);
                } else {
                    HookStatus.recoveredViewId(family, name);
                    wanted.add(id);
                }
            }
            int within = 0;
            if (scope != null) {
                within = id(owner, scope);
                if (within == 0) {
                    HookStatus.missingViewId(family, scope);
                    return false;
                }
                HookStatus.recoveredViewId(family, scope);
            }
            if (wanted.isEmpty()) return false;
            return walk(owner.getWindow().getDecorView(), scope == null, within, wanted, new int[] {MAX_VIEWS});
        }

        private boolean walk(View view, boolean inside, int within, Set<Integer> wanted, int[] budget) {
            if (budget[0]-- <= 0) return false;
            boolean found = false;
            int id = view.getId();
            if (id != View.NO_ID) {
                if (id == within) inside = true;
                if (inside && wanted.contains(id) && !(view instanceof ViewStub)
                        && view.getVisibility() != View.GONE) {
                    hidden.add(new WeakReference<>(view));
                    view.setVisibility(View.GONE);
                    HookStatus.counted(family, counted);
                    found = true;
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    found |= walk(group.getChildAt(i), inside, within, wanted, budget);
                }
            }
            return found;
        }
    }

    private final class Watcher implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityResumed(@NonNull Activity activity) {
            watch(activity);
        }

        @Override public void onActivityPaused(@NonNull Activity activity) {
            unwatch(activity);
        }

        @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) { }
        @Override public void onActivityStarted(@NonNull Activity activity) { }
        @Override public void onActivityStopped(@NonNull Activity activity) { }
        @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
        @Override public void onActivityDestroyed(@NonNull Activity activity) { }
    }
}
