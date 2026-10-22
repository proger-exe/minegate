package dev.sh3lby.minegate.config;

import java.util.Map;

public class PluginConfig {
    private TelegramConfig telegram;
    private BridgeConfig bridge;
    private ReplyConfig reply;
    private EventsConfig events;
    private MessageFormatsConfig formats;
    private Map<String, CustomCommandConfig> customCommands;

    public TelegramConfig getTelegram() {
        return telegram;
    }

    public void setTelegram(TelegramConfig telegram) {
        this.telegram = telegram;
    }

    public BridgeConfig getBridge() {
        return bridge;
    }

    public void setBridge(BridgeConfig bridge) {
        this.bridge = bridge;
    }

    public ReplyConfig getReply() {
        return reply;
    }

    public void setReply(ReplyConfig reply) {
        this.reply = reply;
    }

    public EventsConfig getEvents() {
        return events;
    }

    public void setEvents(EventsConfig events) {
        this.events = events;
    }

    public MessageFormatsConfig getFormats() {
        return formats;
    }

    public void setFormats(MessageFormatsConfig formats) {
        this.formats = formats;
    }

    public Map<String, CustomCommandConfig> getCustomCommands() {
        return customCommands;
    }

    public void setCustomCommands(Map<String, CustomCommandConfig> customCommands) {
        this.customCommands = customCommands;
    }

    public static class TelegramConfig {
        private String token;
        private String chatId;
        private int pollTimeoutSeconds;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public int getPollTimeoutSeconds() {
            return pollTimeoutSeconds;
        }

        public void setPollTimeoutSeconds(int pollTimeoutSeconds) {
            this.pollTimeoutSeconds = pollTimeoutSeconds;
        }
    }

    public static class BridgeConfig {
        private boolean enabled;
        private boolean readOnly;
        private long cooldownMs;
        private int maxMessageLength;
        private boolean enableColorCodes = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public long getCooldownMs() {
            return cooldownMs;
        }

        public void setCooldownMs(long cooldownMs) {
            this.cooldownMs = cooldownMs;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }

        public boolean isEnableColorCodes() {
            return enableColorCodes;
        }

        public void setEnableColorCodes(boolean enableColorCodes) {
            this.enableColorCodes = enableColorCodes;
        }
    }

    public static class ReplyConfig {
        private boolean enabled;
        private long maxAgeSeconds;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }
    }

    public static class EventsConfig {
        private boolean join;
        private boolean quit;
        private boolean death;
        private boolean server;

        public boolean isJoin() {
            return join;
        }

        public void setJoin(boolean join) {
            this.join = join;
        }

        public boolean isQuit() {
            return quit;
        }

        public void setQuit(boolean quit) {
            this.quit = quit;
        }

        public boolean isDeath() {
            return death;
        }

        public void setDeath(boolean death) {
            this.death = death;
        }

        public boolean isServer() {
            return server;
        }

        public void setServer(boolean server) {
            this.server = server;
        }
    }

    public static class MessageFormatsConfig {
        private String minecraftToTelegram = "⛏ {player}: {message}";
        private String telegramToMinecraft = "[TG] {name}: {message}";
        private String systemMessage = "⚙ {message}";
        private String replyIndicator = "[в ответ {player}]";
        private String replyFormat = "@{telegramName} {replyIndicator}: {message}";

        public String getMinecraftToTelegram() {
            return minecraftToTelegram;
        }

        public void setMinecraftToTelegram(String minecraftToTelegram) {
            this.minecraftToTelegram = minecraftToTelegram;
        }

        public String getTelegramToMinecraft() {
            return telegramToMinecraft;
        }

        public void setTelegramToMinecraft(String telegramToMinecraft) {
            this.telegramToMinecraft = telegramToMinecraft;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }

        public String getReplyIndicator() {
            return replyIndicator;
        }

        public void setReplyIndicator(String replyIndicator) {
            this.replyIndicator = replyIndicator;
        }

        public String getReplyFormat() {
            return replyFormat;
        }

        public void setReplyFormat(String replyFormat) {
            this.replyFormat = replyFormat;
        }
    }

    public static class CustomCommandConfig {
        private boolean needPlayer;
        private String command;
        private String description;
        private int maxOutputLines = 15;

        public boolean isNeedPlayer() {
            return needPlayer;
        }

        public void setNeedPlayer(boolean needPlayer) {
            this.needPlayer = needPlayer;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getMaxOutputLines() {
            return maxOutputLines;
        }

        public void setMaxOutputLines(int maxOutputLines) {
            this.maxOutputLines = maxOutputLines;
        }
    }
}
