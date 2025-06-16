package part.example.chess.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TokenCache {
    private static final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 300_000; // 5 минут

    private static final Map<String, Boolean> validTokens = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static {

        scheduler.scheduleAtFixedRate(validTokens::clear, 5, 5, TimeUnit.MINUTES);
    }

    public static boolean isValid(String token) {
        // Если токен есть в кеше - возвращаем его статус
        if (validTokens.containsKey(token)) {
            return validTokens.get(token);
        }

        // Если нет - проверяем через auth-сервис
        boolean isValid = validateWithAuthService(token);
        validTokens.put(token, isValid);

        return isValid;
    }

    private static boolean validateWithAuthService(String token) {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/auth/validate"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.body());
                return jsonNode.path("valid").asBoolean() ||
                        "valid".equals(jsonNode.path("status").asText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Периодическая очистка кеша
    static {
        new Timer().schedule(new TimerTask() {
            public void run() {
                cache.clear();
            }
        }, CACHE_DURATION_MS, CACHE_DURATION_MS);
    }
}
