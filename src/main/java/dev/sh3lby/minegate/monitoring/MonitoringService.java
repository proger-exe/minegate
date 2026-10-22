package dev.sh3lby.minegate.monitoring;

import dev.sh3lby.minegate.bridge.ChatBridge;
import dev.sh3lby.minegate.config.PluginConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MonitoringService {
    private static final long POLL_TIMEOUT_MS = 250L;

    private final BlockingQueue<MonitoringEvent> queue;
    private final Map<UUID, Long> lastMessageTime;
    private final ChatBridge chatBridge;
    private final PluginConfig config;
    private final Logger logger;

    private Thread workerThread;
    private volatile boolean running = false;

    public MonitoringService(ChatBridge chatBridge, PluginConfig config, Logger logger) {
        this.queue = new LinkedBlockingQueue<>();
        this.lastMessageTime = new HashMap<>();
        this.chatBridge = chatBridge;
        this.config = config;
        this.logger = logger;
    }

    public void start() {
        if (running) {
            logger.warning("Monitoring service is already running");
            return;
        }

        running = true;
        workerThread = new Thread(this::workerLoop, "MineGate-Monitoring");
        workerThread.setDaemon(true);
        workerThread.start();
        logger.info("Monitoring service started");
    }

    public void enqueue(MonitoringEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while enqueueing monitoring event");
        }
    }

    public void stop(long drainTimeoutMillis) {
        if (workerThread == null) {
            running = false;
            return;
        }

        running = false;

        try {
            workerThread.join(drainTimeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (workerThread.isAlive()) {
            workerThread.interrupt();
            try {
                workerThread.join(Math.min(POLL_TIMEOUT_MS, Math.max(drainTimeoutMillis, 0L)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (workerThread.isAlive()) {
            logger.warning("Monitoring worker did not stop cleanly before shutdown");
        } else {
            logger.info("Monitoring service stopped");
        }

        workerThread = null;
    }

    private void workerLoop() {
        while (running || !queue.isEmpty()) {
            MonitoringEvent event = pollEvent();
            if (event == null) {
                if (!running && Thread.currentThread().isInterrupted()) {
                    break;
                }
                continue;
            }

            processEvent(event);
            if (!running && Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private MonitoringEvent pollEvent() {
        try {
            return queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void processEvent(MonitoringEvent event) {
        switch (event.getType()) {
            case PLAYER_JOIN -> processPlayerJoin(event);
            case PLAYER_QUIT -> processPlayerQuit(event);
            case PLAYER_DEATH -> processPlayerDeath(event);
            case PLAYER_CHAT -> processPlayerChat(event);
            case SERVER_START -> processServerStart();
            case SERVER_STOP -> processServerStop();
            default -> logger.warning("Unknown monitoring event: " + event.getType());
        }
    }

    private void processPlayerJoin(MonitoringEvent event) {
        if (config.getEvents() != null && config.getEvents().isJoin()) {
            chatBridge.sendSystemToTelegram(event.getPlayerName() + " зашёл на сервер");
        }
    }

    private void processPlayerQuit(MonitoringEvent event) {
        if (config.getEvents() != null && config.getEvents().isQuit()) {
            chatBridge.sendSystemToTelegram(event.getPlayerName() + " вышел с сервера");
        }
    }

    private void processPlayerDeath(MonitoringEvent event) {
        if (config.getEvents() != null && config.getEvents().isDeath() && event.getMessage() != null) {
            chatBridge.sendSystemToTelegram(event.getMessage());
        }
    }

    private void processPlayerChat(MonitoringEvent event) {
        if (!config.getBridge().isEnabled()) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }

        UUID playerId = event.getPlayerId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerId);
        if (lastTime != null && (currentTime - lastTime) < config.getBridge().getCooldownMs()) {
            return;
        }

        lastMessageTime.put(playerId, currentTime);

        if (message.length() > config.getBridge().getMaxMessageLength()) {
            message = message.substring(0, config.getBridge().getMaxMessageLength() - 3) + "...";
        }

        chatBridge.sendMinecraftToTelegram(event.getPlayerName(), message);
    }

    private void processServerStart() {
        if (config.getEvents() != null && config.getEvents().isServer()) {
            chatBridge.sendSystemToTelegram("Сервер запущен");
        }
    }

    private void processServerStop() {
        if (config.getEvents() != null && config.getEvents().isServer()) {
            chatBridge.sendSystemToTelegram("Сервер остановлен");
        }
    }

    private String extractTelegramMessage(String rawMessage) {
        if (rawMessage == null) {
            return null;
        }

        int prefixIndex = skipLeadingNoise(rawMessage, 0);
        if (prefixIndex >= rawMessage.length() || rawMessage.charAt(prefixIndex) != '!') {
            return null;
        }

        int contentIndex = skipLeadingNoise(rawMessage, prefixIndex + 1);
        if (contentIndex >= rawMessage.length()) {
            return null;
        }

        return rawMessage.substring(contentIndex);
    }

    private int skipLeadingNoise(String value, int startIndex) {
        int index = startIndex;
        while (index < value.length()) {
            char ch = value.charAt(index);
            int charType = Character.getType(ch);
            if (Character.isWhitespace(ch)
                    || Character.isSpaceChar(ch)
                    || Character.isISOControl(ch)
                    || charType == Character.FORMAT) {
                index++;
                continue;
            }
            break;
        }
        return index;
    }
}
