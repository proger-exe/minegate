package dev.sh3lby.minegate.bridge;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MessageQueue {
    private final BlockingQueue<QueuedMessage> queue;
    private final Logger logger;

    public MessageQueue(Logger logger) {
        this.queue = new LinkedBlockingQueue<>();
        this.logger = logger;
    }

    public void enqueue(QueuedMessage message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Interrupted while enqueueing message");
        }
    }

    public QueuedMessage dequeue() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public QueuedMessage poll(long timeoutMillis) {
        try {
            return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public static class QueuedMessage {
        private final String text;
        private final MessageSource source;
        private final Long replyToMessageId;
        private final String minecraftMessageId;
        private final String minecraftPlayerName;

        public QueuedMessage(String text, MessageSource source) {
            this(text, source, null, null, null);
        }

        public QueuedMessage(String text, MessageSource source, Long replyToMessageId) {
            this(text, source, replyToMessageId, null, null);
        }

        public QueuedMessage(String text, MessageSource source, Long replyToMessageId,
                             String minecraftMessageId, String minecraftPlayerName) {
            this.text = text;
            this.source = source;
            this.replyToMessageId = replyToMessageId;
            this.minecraftMessageId = minecraftMessageId;
            this.minecraftPlayerName = minecraftPlayerName;
        }

        public String getText() {
            return text;
        }

        public MessageSource getSource() {
            return source;
        }

        public Long getReplyToMessageId() {
            return replyToMessageId;
        }

        public String getMinecraftMessageId() {
            return minecraftMessageId;
        }

        public String getMinecraftPlayerName() {
            return minecraftPlayerName;
        }
    }

    public enum MessageSource {
        MINECRAFT,
        TELEGRAM,
        SYSTEM
    }
}
