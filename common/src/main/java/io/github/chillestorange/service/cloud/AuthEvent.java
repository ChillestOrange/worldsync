package io.github.chillestorange.service.cloud;

import java.util.Objects;

/**
 * Events an {@link OAuth2Authenticator} reports back to whatever is observing
 * its login flow. Defined here in the service layer — not in {@code client.ui}
 * — so the authenticator only ever depends on {@code java.util.function.Consumer}
 * to emit these; it never has to know the UI layer exists.
 * <p>
 * Sealed + records rather than a listener interface with one method per event:
 * adding a new kind of event is a single new record, and any {@code switch}
 * over {@code AuthEvent} is checked exhaustively by the compiler — a forgotten
 * case is a build failure, not a silently-ignored no-op at runtime.
 */
public sealed interface AuthEvent {

    /**
     * A browser window could not be opened automatically; the user needs to
     * visit {@code authUrl} themselves to continue the login flow.
     */
    record BrowserUnavailable(String authUrl) implements AuthEvent {
        public BrowserUnavailable {
            Objects.requireNonNull(authUrl, "authUrl");
        }
    }

    /**
     * A token exchange — either the initial authorization-code exchange or a
     * routine refresh — just completed successfully. Carries no data; its
     * presence is the signal.
     */
    record Complete() implements AuthEvent {
    }
}
