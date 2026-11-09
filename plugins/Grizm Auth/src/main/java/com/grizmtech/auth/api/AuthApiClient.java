package com.grizmtech.auth.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grizmtech.auth.GrizmAuth;
import com.grizmtech.auth.config.Settings;
import com.grizmtech.auth.model.UserProfileData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AuthApiClient {

    private final GrizmAuth plugin;
    private final Settings settings;
    private final HttpClient httpClient;
    private final Gson gson;

    public AuthApiClient(GrizmAuth plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(settings.timeoutSeconds))
                .build();
        this.gson = new Gson();
    }

    /**
     * Oyuncunun sunucuya katılmadan önce Launcher oturumunu doğrular.
     */
    public CompletableFuture<AuthVerifyResult> verifyPlayerJoinAsync(String username, String ip) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "verify_join");
        payload.addProperty("username", username);
        payload.addProperty("ip", ip);

        return sendPostWithFallback(payload).thenApply(json -> {
            if (json == null) {
                return AuthVerifyResult.failed("Web API sunucusuna baglanilamadi.", false);
            }

            String status = json.has("status") ? json.get("status").getAsString() : "error";
            boolean authenticated = json.has("authenticated") && json.get("authenticated").getAsBoolean();
            boolean launcherVerified = json.has("launcher_verified") && json.get("launcher_verified").getAsBoolean();
            boolean registeredOnWeb = json.has("registered_on_web") && json.get("registered_on_web").getAsBoolean();
            String message = json.has("message") ? json.get("message").getAsString() : "";

            if ("success".equalsIgnoreCase(status) && authenticated) {
                String email = "";
                String fullName = username;
                String avatarUrl = "";
                String sessionId = json.has("session_id") ? json.get("session_id").getAsString() : "";

                if (json.has("user") && json.get("user").isJsonObject()) {
                    JsonObject userObj = json.getAsJsonObject("user");
                    if (userObj.has("email")) email = userObj.get("email").getAsString();
                    if (userObj.has("full_name")) fullName = userObj.get("full_name").getAsString();
                    if (userObj.has("avatar")) avatarUrl = userObj.get("avatar").getAsString();
                }

                UserProfileData profile = new UserProfileData(username, email, fullName, avatarUrl, sessionId);
                return AuthVerifyResult.success(launcherVerified, profile);
            } else {
                return AuthVerifyResult.failed(message, registeredOnWeb);
            }
        });
    }

    /**
     * Oyuncu tek kullanımlık web kodunu girdiğinde (/login <code>) doğrular.
     */
    public CompletableFuture<AuthVerifyResult> verifyCodeAsync(String username, String code) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "verify_code");
        payload.addProperty("username", username);
        payload.addProperty("code", code);

        return sendPostWithFallback(payload).thenApply(json -> {
            if (json == null) {
                return AuthVerifyResult.failed("API baglanti hatasi.", false);
            }

            String status = json.has("status") ? json.get("status").getAsString() : "error";
            boolean authenticated = json.has("authenticated") && json.get("authenticated").getAsBoolean();
            String message = json.has("message") ? json.get("message").getAsString() : "";

            if ("success".equalsIgnoreCase(status) && authenticated) {
                UserProfileData profile = new UserProfileData(username, "", username, "", "CODE-" + code);
                return AuthVerifyResult.success(false, profile);
            }
            return AuthVerifyResult.failed(message, true);
        });
    }

    /**
     * Oyuncu oyundan çıktığında Web API oturumunu günceller.
     */
    public void notifyPlayerQuitAsync(String username) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "player_quit");
        payload.addProperty("username", username);

        sendPostWithFallback(payload).exceptionally(ex -> null);
    }

    /**
     * Web API Sağlık Kontrolü
     */
    public CompletableFuture<Boolean> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(settings.apiUrl + "?action=status"))
                        .timeout(Duration.ofSeconds(settings.timeoutSeconds))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private CompletableFuture<JsonObject> sendPostWithFallback(JsonObject payload) {
        String jsonBody = gson.toJson(payload);

        return sendHttpPost(settings.apiUrl, jsonBody).thenCompose(result -> {
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }
            // Yedek URL'yi dene
            if (settings.fallbackUrl != null && !settings.fallbackUrl.isEmpty() && !settings.fallbackUrl.equalsIgnoreCase(settings.apiUrl)) {
                return sendHttpPost(settings.fallbackUrl, jsonBody);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<JsonObject> sendHttpPost(String targetUrl, String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .timeout(Duration.ofSeconds(settings.timeoutSeconds))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("User-Agent", "GrizmAuth-Minecraft-1.21")
                        .POST(HttpRequest.BodyPublishers.ofString(body));

                if (settings.serverKey != null && !settings.serverKey.isEmpty()) {
                    builder.header("X-Server-Key", settings.serverKey);
                }

                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 || response.statusCode() == 400 || response.statusCode() == 404) {
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Web API baglanti hatasi (" + targetUrl + "): " + ex.getMessage());
            }
            return null;
        });
    }
}
