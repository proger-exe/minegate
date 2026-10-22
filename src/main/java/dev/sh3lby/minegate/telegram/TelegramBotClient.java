package dev.sh3lby.minegate.telegram;

import com.google.gson.Gson;
import dev.sh3lby.minegate.config.PluginConfig;
import dev.sh3lby.minegate.telegram.model.GetUpdatesResponse;
import dev.sh3lby.minegate.telegram.model.SendMessageResponse;
import dev.sh3lby.minegate.telegram.model.TelegramResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class TelegramBotClient {
    private static final String API_BASE_URL = "https://api.telegram.org/bot";
    private static final Gson GSON = new Gson();

    private final String token;
    private final String chatId;
    private final HttpClient httpClient;
    private final Logger logger;

    public TelegramBotClient(PluginConfig config, Logger logger) {
        this.token = config.getTelegram().getToken();
        this.chatId = config.getTelegram().getChatId();
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public SendMessageResponse sendMessage(String text, Long replyToMessageId) {
        try {
            String url = API_BASE_URL + token + "/sendMessage";
            
            StringBuilder body = new StringBuilder();
            body.append("chat_id=").append(URLEncoder.encode(chatId, StandardCharsets.UTF_8));
            body.append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));
            body.append("&parse_mode=HTML");
            
            if (replyToMessageId != null) {
                body.append("&reply_to_message_id=").append(replyToMessageId);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                SendMessageResponse sendResponse = GSON.fromJson(response.body(), SendMessageResponse.class);
                if (sendResponse != null && Boolean.TRUE.equals(sendResponse.getOk())) {
                    return sendResponse;
                } else {
                    TelegramResponse errorResponse = GSON.fromJson(response.body(), TelegramResponse.class);
                    logger.warning("Telegram API error: " + (errorResponse != null ? errorResponse.getDescription() : "Unknown error"));
                }
            } else {
                logger.warning("HTTP error " + response.statusCode() + " when sending message to Telegram");
            }
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to send message to Telegram: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    public GetUpdatesResponse getUpdates(Long offset, Integer timeout) {
        try {
            String url = API_BASE_URL + token + "/getUpdates";
            
            StringBuilder body = new StringBuilder();
            if (offset != null) {
                body.append("offset=").append(offset);
            }
            if (timeout != null) {
                if (body.length() > 0) body.append("&");
                body.append("timeout=").append(timeout);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + (body.length() > 0 ? "?" + body.toString() : "")))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(timeout != null ? timeout + 5 : 35))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                GetUpdatesResponse updatesResponse = GSON.fromJson(response.body(), GetUpdatesResponse.class);
                if (updatesResponse != null && Boolean.TRUE.equals(updatesResponse.getOk())) {
                    return updatesResponse;
                } else {
                    TelegramResponse errorResponse = GSON.fromJson(response.body(), TelegramResponse.class);
                    logger.warning("Telegram API error: " + (errorResponse != null ? errorResponse.getDescription() : "Unknown error"));
                }
            } else {
                logger.warning("HTTP error " + response.statusCode() + " when getting updates from Telegram");
            }
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to get updates from Telegram: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    public boolean deleteWebhook() {
        try {
            String url = API_BASE_URL + token + "/deleteWebhook";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                TelegramResponse telegramResponse = GSON.fromJson(response.body(), TelegramResponse.class);
                if (telegramResponse != null && Boolean.TRUE.equals(telegramResponse.getOk())) {
                    logger.info("Webhook deleted successfully");
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.warning("Failed to delete webhook (this is OK if no webhook was set): " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    public String getChatId() {
        return chatId;
    }
}
