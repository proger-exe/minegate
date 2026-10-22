package dev.sh3lby.minegate.bridge;

import dev.sh3lby.minegate.config.PluginConfig;
import dev.sh3lby.minegate.telegram.TelegramBotClient;
import dev.sh3lby.minegate.telegram.model.Message;
import dev.sh3lby.minegate.util.NameResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class ChatBridge {
    private final MessageQueue messageQueue;
    private final ReplyStorage replyStorage;
    private final TelegramBotClient telegramClient;
    private final PluginConfig config;
    private final Logger logger;
    private final dev.sh3lby.minegate.util.MessageFormatter formatter;

    public ChatBridge(MessageQueue messageQueue, ReplyStorage replyStorage, 
                     TelegramBotClient telegramClient, PluginConfig config, Logger logger) {
        this.messageQueue = messageQueue;
        this.replyStorage = replyStorage;
        this.telegramClient = telegramClient;
        this.config = config;
        this.logger = logger;
        this.formatter = new dev.sh3lby.minegate.util.MessageFormatter(config.getFormats());
    }

    public void sendMinecraftToTelegram(String playerName, String message) {
        if (!config.getBridge().isEnabled()) {
            return;
        }

        String formatted = formatter.formatMinecraftToTelegram(playerName, message);
        formatted = dev.sh3lby.minegate.util.MessageFormatter.truncate(formatted, config.getBridge().getMaxMessageLength());
        String minecraftMessageId = config.getReply().isEnabled() ? playerName + ":" + message : null;
        String minecraftPlayerName = config.getReply().isEnabled() ? playerName : null;

        messageQueue.enqueue(new MessageQueue.QueuedMessage(
                formatted,
                MessageQueue.MessageSource.MINECRAFT,
                null,
                minecraftMessageId,
                minecraftPlayerName
        ));
    }

    public void sendSystemToTelegram(String message) {
        if (!config.getBridge().isEnabled()) {
            return;
        }

        String formatted = formatter.formatSystemMessage(message);
        messageQueue.enqueue(new MessageQueue.QueuedMessage(formatted, MessageQueue.MessageSource.SYSTEM));
    }

    public void processTelegramMessage(Message telegramMessage) {
        if (!config.getBridge().isEnabled() || config.getBridge().isReadOnly()) {
            return;
        }

        // Ignore messages from bots
        if (telegramMessage.getFrom() != null && Boolean.TRUE.equals(telegramMessage.getFrom().getIsBot())) {
            return;
        }

        // Ignore messages not from configured chat
        if (telegramMessage.getChat() == null || 
            !telegramClient.getChatId().equals(String.valueOf(telegramMessage.getChat().getId()))) {
            return;
        }

        String text = telegramMessage.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String telegramName = NameResolver.resolveTelegramName(telegramMessage.getFrom());
        boolean enableColorCodes = config.getBridge().isEnableColorCodes();

        // Check for reply context
        Component messageComponent;
        if (config.getReply().isEnabled() && telegramMessage.getReplyToMessage() != null) {
            Long replyToMessageId = telegramMessage.getReplyToMessage().getMessageId();
            ReplyStorage.ReplyMapping mapping = replyStorage.getByTelegramMessageId(replyToMessageId);
            
            if (mapping != null) {
                // Format with reply context
                String originalPlayerName = mapping.getPlayerName();
                if (originalPlayerName == null) {
                    // Fallback: try to extract from original text
                    originalPlayerName = extractPlayerNameFromOriginal(mapping.getOriginalText());
                }
                
                if (originalPlayerName != null) {
                    // Extract original message text (remove formatting prefix)
                    String originalMessage = mapping.getOriginalText();
                    String playerPrefix = formatter.formatMinecraftToTelegram(originalPlayerName, "");
                    if (originalMessage.startsWith(playerPrefix)) {
                        originalMessage = originalMessage.substring(playerPrefix.length());
                    }
                    
                    // Format reply indicator with color codes support (preserve colors from template)
                    Component replyIndicatorBase = formatter.formatReplyIndicatorComponent(originalPlayerName, enableColorCodes);
                    Component replyIndicator = replyIndicatorBase
                            .decoration(TextDecoration.ITALIC, true)
                            .hoverEvent(HoverEvent.showText(Component.text(originalPlayerName + ": " + originalMessage)));
                    
                    // Parse message text (color codes)
                    Component textComponent = dev.sh3lby.minegate.util.MessageFormatter.parseFormattedText(
                        text,
                        enableColorCodes
                    );
                    
                    // Use reply-format template from config with color codes support
                    // Build component using formatter's method
                    messageComponent = formatter.formatReplyComponent(
                        telegramName,
                        replyIndicator,
                        textComponent,
                        enableColorCodes
                    );
                } else {
                    // Use formatTelegramToMinecraftComponent which supports color codes in template
                    messageComponent = formatter.formatTelegramToMinecraftComponent(
                        telegramName,
                        text,
                        enableColorCodes
                    );
                }
            } else {
                // Reply context lost or expired - use format with color codes support
                messageComponent = formatter.formatTelegramToMinecraftComponent(
                    telegramName,
                    text,
                    enableColorCodes
                );
            }
        } else {
            // Use formatTelegramToMinecraftComponent which supports color codes in template
            messageComponent = formatter.formatTelegramToMinecraftComponent(
                telegramName,
                text,
                enableColorCodes
            );
        }

        // Send to all players in main thread
        final Component finalComponent = messageComponent;
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("MineGate");
        if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(finalComponent);
            }
                // Also log to console
                Bukkit.getConsoleSender().sendMessage(finalComponent);
            });
        }
    }

    private String extractPlayerNameFromOriginal(String originalText) {
        // Extract player name from formatted text like "⛏ PlayerName: message"
        if (originalText != null && originalText.startsWith("⛏ ")) {
            int colonIndex = originalText.indexOf(":");
            if (colonIndex > 2) {
                return originalText.substring(2, colonIndex).trim();
            }
        }
        return null;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }
}
