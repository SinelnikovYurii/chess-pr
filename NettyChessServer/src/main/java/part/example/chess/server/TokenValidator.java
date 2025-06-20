package part.example.chess.server;

import io.netty.handler.codec.http.FullHttpRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class TokenValidator {
    private static final String AUTH_SERVICE_URL = "http://localhost:8081/api/auth/validate";

    public boolean validateToken(String token) {
        try {
            URL url = new URL(AUTH_SERVICE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                return isTokenValid(response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean isTokenValid(String response) {
        return response.contains("\"status\":\"valid\"");
    }
}
