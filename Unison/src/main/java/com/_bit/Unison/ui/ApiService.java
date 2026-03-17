package com._bit.Unison.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiService {

    public static boolean login(String userId, String name) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String json;

            if (userId == null || userId.isEmpty()) {
                json = String.format(
                        "{\"displayName\":\"%s\",\"courseIds\":[]}",
                        name
                );
            } else {
                json = String.format(
                        "{\"userId\":\"%s\"}",
                        userId
                );
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 201) {
                String body = response.body();

                String sessionId = body.split("\"sessionId\":\"")[1].split("\"")[0];
                String userIdRes = body.split("\"userId\":\"")[1].split("\"")[0];

                SessionManager.sessionId = sessionId;
                SessionManager.userId = userIdRes;

                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getCurrentUser() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/me"))
                    .header("X-Session-Id", SessionManager.sessionId)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}