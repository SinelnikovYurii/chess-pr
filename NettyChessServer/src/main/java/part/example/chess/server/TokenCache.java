package part.example.chess.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TokenCache {
    private static final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 300_000; // 5 минут

    static {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(TokenCache::clearCache, CACHE_DURATION_MS, CACHE_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    public static boolean isValid(String token) {
        return cache.getOrDefault(token, false);
    }

    public static void setValid(String token, boolean valid) {
        cache.put(token, valid);
    }

    public static void clearCache() {
        cache.clear();
    }
}