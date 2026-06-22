package io.github.chillestorange.service.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import io.github.chillestorange.logging.WorldSyncLogger;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared OAuth2 "installed app" flow — the loopback-server-plus-browser dance,
 * token storage, and refresh logic that Google, Dropbox, and Microsoft's OAuth2
 * implementations all need in basically the same shape. A new OAuth-style
 * provider only needs to extend this and supply its endpoint/scope constants
 * via the constructor; none of the fiddly parts need reimplementing.
 */
public abstract class OAuth2Authenticator implements CloudAuthenticator {

    private final String authEndpoint;
    private final String tokenEndpoint;
    private final String scope;
    private final String clientId;
    private final String clientSecret;
    private final Path tokenStorePath;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Instant accessTokenExpiry = Instant.EPOCH;

    protected OAuth2Authenticator(
            String authEndpoint, String tokenEndpoint, String scope,
            String clientId, String clientSecret, Path tokenStorePath, HttpClient httpClient
    ) {
        this.authEndpoint = authEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.scope = scope;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenStorePath = tokenStorePath;
        this.httpClient = httpClient;
        loadStoredTokens();
    }

    /** Returns a valid access token, refreshing or running the full consent flow as needed. */
    @Override
    public synchronized String getValidAccessToken() throws IOException, InterruptedException {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiry.minusSeconds(60))) {
            return accessToken;
        }
        if (refreshToken != null) {
            try {
                WorldSyncLogger.debug("Refreshing access token...");
                refreshAccessToken();
                return accessToken;
            } catch (IOException e) {
                WorldSyncLogger.warn("Refresh token rejected, falling back to full login: " + e.getMessage());
            }
        }
        runAuthorizationFlow();
        return accessToken;
    }

    private void loadStoredTokens() {
        if (!Files.exists(tokenStorePath)) return;
        try {
            String json = Files.readString(tokenStorePath, StandardCharsets.UTF_8);
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            if (obj.has("refresh_token")) refreshToken = obj.get("refresh_token").getAsString();
            if (obj.has("access_token"))  accessToken  = obj.get("access_token").getAsString();
            if (obj.has("expiry"))        accessTokenExpiry = Instant.parse(obj.get("expiry").getAsString());
        } catch (IOException e) {
            WorldSyncLogger.warn("Could not read stored credentials: " + e.getMessage());
        }
    }

    private void persistTokens() {
        JsonObject obj = new JsonObject();
        obj.addProperty("access_token", accessToken);
        obj.addProperty("refresh_token", refreshToken);
        obj.addProperty("expiry", accessTokenExpiry.toString());
        try {
            Path parent = tokenStorePath.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(tokenStorePath, gson.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            WorldSyncLogger.warn("Could not persist credentials: " + e.getMessage());
        }
    }

    private void refreshAccessToken() throws IOException, InterruptedException {
        String body = "client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&refresh_token=" + enc(refreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Token refresh failed: HTTP " + response.statusCode() + " " + response.body());
        }
        applyTokenResponse(response.body());
    }

    private void runAuthorizationFlow() throws IOException, InterruptedException {
        AtomicReference<String> capturedCode = new AtomicReference<>();
        CompletableFuture<Void> callbackReceived = new CompletableFuture<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port + "/callback";

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = extractParam(query, "code");
            capturedCode.set(code);

            String html = code != null
                    ? "<html><body>WorldSync authorized — you can close this tab.</body></html>"
                    : "<html><body>WorldSync authorization failed — check the mod log.</body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
            callbackReceived.complete(null);
        });
        server.start();

        try {
            String authUrl = authEndpoint
                    + "?client_id=" + enc(clientId)
                    + "&redirect_uri=" + enc(redirectUri)
                    + "&response_type=code"
                    + "&scope=" + enc(scope)
                    + "&access_type=offline"
                    + "&prompt=consent";

            WorldSyncLogger.info("Opening browser for authorization...");
            openBrowser(authUrl);

            try {
                callbackReceived.get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IOException("Timed out waiting for authorization", e);
            }
        } finally {
            server.stop(0);
        }

        String code = capturedCode.get();
        if (code == null) {
            throw new IOException("Authorization did not return a code");
        }

        String body = "client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Token exchange failed: HTTP " + response.statusCode() + " " + response.body());
        }
        applyTokenResponse(response.body());
    }

    private void applyTokenResponse(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        accessToken = obj.get("access_token").getAsString();
        if (obj.has("refresh_token")) {
            refreshToken = obj.get("refresh_token").getAsString();
        }
        int expiresIn = obj.has("expires_in") ? obj.get("expires_in").getAsInt() : 3600;
        accessTokenExpiry = Instant.now().plusSeconds(expiresIn);
        persistTokens();

        // Whether this came from a fresh authorization-code exchange or a routine
        // refresh, the authenticator is now in a "good" state. Any UI showing an
        // auth-required prompt should stand down — that's exactly what this event
        // is for. publish() is a no-op if nothing is currently subscribed.
        AuthEventBus.publish(new AuthEvent.Complete());
    }

    private static void openBrowser(String url) throws IOException {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
        } else {
            WorldSyncLogger.debug("Cannot open a browser automatically. Visit this URL manually: {}", url);
            AuthEventBus.publish(new AuthEvent.BrowserUnavailable(url));
        }
    }

    private static String extractParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            if (pair.substring(0, eq).equals(key)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}