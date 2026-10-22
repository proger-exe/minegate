package dev.sh3lby.minegate.telegram;

import dev.sh3lby.minegate.bridge.MessageQueue;
import dev.sh3lby.minegate.bridge.ReplyStorage;
import dev.sh3lby.minegate.telegram.model.SendMessageResponse;

import java.util.logging.Logger;

public class TelegramOutboundService {
    private static final long POLL_TIMEOUT_MS = 250L;

    private final MessageQueue messageQueue;
    private final ReplyStorage replyStorage;
    private final TelegramBotClient telegramClient;
    private final Logger logger;

    private Thread workerThread;
    private volatile boolean running = false;

    public TelegramOutboundService(MessageQueue messageQueue, ReplyStorage replyStorage,
                                  TelegramBotClient telegramClient, Logger logger) {
        this.messageQueue = messageQueue;
        this.replyStorage = replyStorage;
        this.telegramClient = telegramClient;
        this.logger = logger;
    }

    public void start() {
        if (running) {
            logger.warning("Telegram outbound service is already running");
            return;
        }

        running = true;
        workerThread = new Thread(this::workerLoop, "MineGate-TelegramOutbound");
        workerThread.setDaemon(true);
        workerThread.start();
        logger.info("Telegram outbound service started");
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
            logger.warning("Telegram outbound worker did not stop cleanly before shutdown");
        } else {
            logger.info("Telegram outbound service stopped");
        }

        workerThread = null;
    }

    private void workerLoop() {
        while (running || !messageQueue.isEmpty()) {
            MessageQueue.QueuedMessage message = messageQueue.poll(POLL_TIMEOUT_MS);
            if (message == null) {
                if (!running && Thread.currentThread().isInterrupted()) {
                    break;
                }
                continue;
            }

            processMessage(message);
            if (!running && Thread.currentThread().isInterrupted()) {
                break;
            }
        }
    }

    private void processMessage(MessageQueue.QueuedMessage message) {
        SendMessageResponse response = telegramClient.sendMessage(message.getText(), message.getReplyToMessageId());
        if (response == null || response.getResult() == null) {
            return;
        }

        if (message.getSource() != MessageQueue.MessageSource.MINECRAFT) {
            return;
        }

        Long telegramMessageId = response.getResult().getMessageId();
        if (telegramMessageId == null || message.getMinecraftMessageId() == null) {
            return;
        }

        replyStorage.storeMapping(
                telegramMessageId,
                message.getMinecraftMessageId(),
                message.getText(),
                message.getMinecraftPlayerName()
        );
    }
}
