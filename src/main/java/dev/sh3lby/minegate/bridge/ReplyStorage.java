package dev.sh3lby.minegate.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ReplyStorage {
    private final Map<Long, ReplyMapping> telegramToMinecraft;
    private final Map<String, ReplyMapping> minecraftToTelegram;
    private final long maxAgeSeconds;
    private final Logger logger;
    private final ScheduledExecutorService cleanupExecutor;

    public ReplyStorage(long maxAgeSeconds, Logger logger) {
        this.telegramToMinecraft = new ConcurrentHashMap<>();
        this.minecraftToTelegram = new ConcurrentHashMap<>();
        this.maxAgeSeconds = maxAgeSeconds;
        this.logger = logger;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MineGate-ReplyCleanup");
            t.setDaemon(true);
            return t;
        });

        // Schedule cleanup task every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    public void storeMapping(Long telegramMessageId, String minecraftMessageId, String originalText) {
        storeMapping(telegramMessageId, minecraftMessageId, originalText, null);
    }

    public void storeMapping(Long telegramMessageId, String minecraftMessageId, String originalText, String playerName) {
        long timestamp = System.currentTimeMillis();
        ReplyMapping mapping = new ReplyMapping(telegramMessageId, minecraftMessageId, originalText, playerName, timestamp);
        
        telegramToMinecraft.put(telegramMessageId, mapping);
        minecraftToTelegram.put(minecraftMessageId, mapping);
    }

    public ReplyMapping getByTelegramMessageId(Long telegramMessageId) {
        ReplyMapping mapping = telegramToMinecraft.get(telegramMessageId);
        if (mapping != null && !mapping.isExpired(maxAgeSeconds)) {
            return mapping;
        }
        if (mapping != null) {
            telegramToMinecraft.remove(telegramMessageId);
            minecraftToTelegram.remove(mapping.getMinecraftMessageId());
        }
        return null;
    }

    public ReplyMapping getByMinecraftMessageId(String minecraftMessageId) {
        ReplyMapping mapping = minecraftToTelegram.get(minecraftMessageId);
        if (mapping != null && !mapping.isExpired(maxAgeSeconds)) {
            return mapping;
        }
        if (mapping != null) {
            minecraftToTelegram.remove(minecraftMessageId);
            telegramToMinecraft.remove(mapping.getTelegramMessageId());
        }
        return null;
    }

    private void cleanup() {
        long currentTime = System.currentTimeMillis();
        long expiredCount = 0;

        telegramToMinecraft.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(maxAgeSeconds)) {
                minecraftToTelegram.remove(entry.getValue().getMinecraftMessageId());
                return true;
            }
            return false;
        });

        if (expiredCount > 0) {
            logger.fine("Cleaned up " + expiredCount + " expired reply mappings");
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class ReplyMapping {
        private final Long telegramMessageId;
        private final String minecraftMessageId;
        private final String originalText;
        private final String playerName;
        private final long timestamp;

        public ReplyMapping(Long telegramMessageId, String minecraftMessageId, String originalText, String playerName, long timestamp) {
            this.telegramMessageId = telegramMessageId;
            this.minecraftMessageId = minecraftMessageId;
            this.originalText = originalText;
            this.playerName = playerName;
            this.timestamp = timestamp;
        }

        public Long getTelegramMessageId() {
            return telegramMessageId;
        }

        public String getMinecraftMessageId() {
            return minecraftMessageId;
        }

        public String getOriginalText() {
            return originalText;
        }

        public String getPlayerName() {
            return playerName;
        }

        public boolean isExpired(long maxAgeSeconds) {
            return (System.currentTimeMillis() - timestamp) > (maxAgeSeconds * 1000);
        }
    }
}
