package dev.sh3lby.minegate.telegram;

import dev.sh3lby.minegate.bridge.ChatBridge;
import dev.sh3lby.minegate.commands.CustomCommandManager;
import dev.sh3lby.minegate.config.PluginConfig;
import dev.sh3lby.minegate.telegram.model.GetUpdatesResponse;
import dev.sh3lby.minegate.telegram.model.Message;
import dev.sh3lby.minegate.telegram.model.Update;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Logger;

public class TelegramPollingService {
    private final TelegramBotClient telegramClient;
    private final ChatBridge chatBridge;
    private final CustomCommandManager commandManager;
    private final PluginConfig config;
    private final Logger logger;
    private Thread pollingThread;
    private volatile boolean running = false;
    private Long lastUpdateId = null;

    public TelegramPollingService(TelegramBotClient telegramClient, ChatBridge chatBridge,
                                 CustomCommandManager commandManager, PluginConfig config, Logger logger) {
        this.telegramClient = telegramClient;
        this.chatBridge = chatBridge;
        this.commandManager = commandManager;
        this.config = config;
        this.logger = logger;
    }

    public void start() {
        if (running) {
            logger.warning("Polling service is already running");
            return;
        }

        running = true;
        pollingThread = new Thread(this::pollingLoop, "MineGate-TelegramPolling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        logger.info("Telegram polling service started");
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
            try {
                pollingThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Telegram polling service stopped");
    }

    private void pollingLoop() {
        int consecutiveErrors = 0;
        int maxConsecutiveErrors = 5;

        telegramClient.deleteWebhook();

        while (running) {
            try {
                Integer timeout = config.getTelegram().getPollTimeoutSeconds();
                Long offset = lastUpdateId != null ? lastUpdateId + 1 : null;

                GetUpdatesResponse response = telegramClient.getUpdates(offset, timeout);

                if (response != null && Boolean.TRUE.equals(response.getOk())) {
                    List<Update> updates = response.getResult();
                    if (updates != null && !updates.isEmpty()) {
                        consecutiveErrors = 0;
                        processUpdates(updates);
                    }
                } else {
                    consecutiveErrors++;
                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        logger.warning("Too many consecutive errors, waiting before retry...");
                        Thread.sleep(30000); // Wait 30 seconds
                        consecutiveErrors = 0;
                    } else {
                        Thread.sleep(5000); // Wait 5 seconds before retry
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                consecutiveErrors++;
                logger.warning("Error in polling loop: " + e.getMessage());
                if (consecutiveErrors >= maxConsecutiveErrors) {
                    try {
                        Thread.sleep(30000);
                        consecutiveErrors = 0;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void processUpdates(List<Update> updates) {
        for (Update update : updates) {
            if (update.getUpdateId() != null) {
                lastUpdateId = update.getUpdateId();
            }

            Message message = update.getMessage();
            if (message == null) {
                message = update.getEditedMessage();
            }

            if (message == null) {
                continue;
            }

            // Check if message is from configured chat
            if (message.getChat() == null || 
                !telegramClient.getChatId().equals(String.valueOf(message.getChat().getId()))) {
                continue;
            }

            String text = message.getText();
            if (text == null || text.isEmpty()) {
                continue;
            }

            // Handle commands
            if (text.startsWith("/")) {
                handleCommand(message);
            } else {
                // Handle regular message
                // Process in main thread
                final Message finalMessage = message;
                org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("MineGate");
                if (plugin != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        chatBridge.processTelegramMessage(finalMessage)
                    );
                }
            }
        }
    }

    private void handleCommand(Message telegramMessage) {
        String text = telegramMessage.getText();
        if (text == null || !text.startsWith("/")) {
            return;
        }

        // Parse command
        String[] parts = text.substring(1).split("\\s+", 2);
        String commandName = parts[0];
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        // Handle /help command
        if ("help".equalsIgnoreCase(commandName)) {
            String helpText = commandManager.getHelpText();
            telegramClient.sendMessage(helpText, telegramMessage.getMessageId());
            return;
        }

        // Ignore unknown commands without replying in Telegram
        if (!commandManager.isCommandAvailable(commandName)) {
            return;
        }

        // Execute command in main thread
        final String finalCommandName = commandName;
        final String[] finalArgs = args;
        final Long replyToId = telegramMessage.getMessageId();

        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("MineGate");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    List<String> result = commandManager.executeCommand(finalCommandName, finalArgs);
                    
                    // Format result
                    StringBuilder response = new StringBuilder();
                    for (String line : result) {
                        if (response.length() > 0) {
                            response.append("\n");
                        }
                        response.append(line);
                    }

                    // Send response
                    if (response.length() > 0) {
                        telegramClient.sendMessage(response.toString(), replyToId);
                    } else {
                        telegramClient.sendMessage("Команда выполнена (нет вывода)", replyToId);
                    }
                } catch (Exception e) {
                    logger.warning("Error executing command: " + e.getMessage());
                    telegramClient.sendMessage("Ошибка выполнения команды: " + e.getMessage(), replyToId);
                }
            });
        }
    }
}
