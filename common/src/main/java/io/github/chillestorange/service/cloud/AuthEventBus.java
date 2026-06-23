package io.github.chillestorange.service.cloud;

import java.util.function.Consumer;

/**
 * Publishes {@link AuthEvent}s from {@link OAuth2Authenticator} to whatever
 * part of the UI is currently interested.
 * <p>
 * This lives in the service layer — the layer that owns {@code AuthEvent} —
 * so {@code OAuth2Authenticator} reaches it with a same-package call, never a
 * cross-layer import. {@link #publish} is package-private: only code inside
 * {@code service.cloud} can emit an event. {@link #subscribe} and
 * {@link #unsubscribe} are public, because it's perfectly fine for the UI
 * layer (e.g. {@code SyncingScreen}) to depend on a service-layer type —
 * that's the direction the dependency arrow is supposed to point. The bug
 * this replaces was the authenticator importing a UI class, not the UI
 * importing a service class.
 * <p>
 * The player can only have one cloud provider selected at a time, so only one
 * {@link OAuth2Authenticator} is ever active and a single volatile listener
 * is sufficient — no per-instance scoping, no queue, no list.
 */
public final class AuthEventBus {

    private static volatile Consumer<AuthEvent> listener;

    private AuthEventBus() {
    }

    /**
     * Registers the active screen's handler. Called from init() so it is
     * automatically re-subscribed if the screen's widgets are ever rebuilt.
     */
    public static void subscribe(Consumer<AuthEvent> listener) {
        AuthEventBus.listener = listener;
    }

    /**
     * Clears the subscription. Called from removed() so a stale callback
     * never fires into a screen that's no longer visible.
     */
    public static void unsubscribe() {
        listener = null;
    }

    /**
     * Called by {@link OAuth2Authenticator} on the sync background thread.
     * Forwards the event to whichever screen is currently subscribed, if any.
     * The subscriber is responsible for marshalling back onto the main thread
     * before touching any widget state.
     */
    static void publish(AuthEvent event) {
        Consumer<AuthEvent> l = listener;
        if (l != null) l.accept(event);
    }
}
