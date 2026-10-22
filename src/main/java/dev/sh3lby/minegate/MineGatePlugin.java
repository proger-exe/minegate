package dev.sh3lby.minegate;

import dev.sh3lby.minegate.bridge.ChatBridge;
import dev.sh3lby.minegate.bridge.MessageQueue;
import dev.sh3lby.minegate.bridge.ReplyStorage;
import dev.sh3lby.minegate.commands.CustomCommandManager;
import dev.sh3lby.minegate.config.ConfigManager;
import dev.sh3lby.minegate.listeners.ChatListener;
import dev.sh3lby.minegate.listeners.EventListener;
import dev.sh3lby.minegate.monitoring.MonitoringEvent;
import dev.sh3lby.minegate.monitoring.MonitoringService;
import dev.sh3lby.minegate.telegram.TelegramBotClient;
import dev.sh3lby.minegate.telegram.TelegramOutboundService;
import dev.sh3lby.minegate.telegram.TelegramPollingService;
import org.bukkit.plugin.java.JavaPlugin;

public class MineGatePlugin extends JavaPlugin {
    private ConfigManager configManager;
    private TelegramBotClient telegramClient;
    private TelegramOutboundService outboundService;
    private TelegramPollingService pollingService;
    private MessageQueue messageQueue;
    private ReplyStorage replyStorage;
    private ChatBridge chatBridge;
    private MonitoringService monitoringService;
    private CustomCommandManager commandManager;
    private ChatListener chatListener;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        getLogger().info("Enabling MineGate plugin...");

        try {
            // Load configuration
            configManager = new ConfigManager(this);
            configManager.loadConfig();

            // Initialize components
            telegramClient = new TelegramBotClient(configManager.getConfig(), getLogger());

            messageQueue = new MessageQueue(getLogger());
            replyStorage = new ReplyStorage(configManager.getConfig().getReply().getMaxAgeSeconds(), getLogger());
            
            chatBridge = new ChatBridge(
                messageQueue,
                replyStorage,
                telegramClient,
                configManager.getConfig(),
                getLogger()
            );
            outboundService = new TelegramOutboundService(messageQueue, replyStorage, telegramClient, getLogger());
            outboundService.start();
            monitoringService = new MonitoringService(chatBridge, configManager.getConfig(), getLogger());
            monitoringService.start();

            commandManager = new CustomCommandManager(configManager.getConfig(), getLogger(), this);

            // Register listeners
            chatListener = new ChatListener(monitoringService);
            getServer().getPluginManager().registerEvents(chatListener, this);

            eventListener = new EventListener(monitoringService);
            getServer().getPluginManager().registerEvents(eventListener, this);

            // Start Telegram polling service
            pollingService = new TelegramPollingService(
                telegramClient,
                chatBridge,
                commandManager,
                configManager.getConfig(),
                getLogger()
            );
            pollingService.start();

            // Send server start message if enabled
            monitoringService.enqueue(MonitoringEvent.serverStart());

            getLogger().info("MineGate plugin enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MineGate plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling MineGate plugin...");

        // Stop polling service
        if (pollingService != null) {
            pollingService.stop();
        }

        if (monitoringService != null) {
            monitoringService.enqueue(MonitoringEvent.serverStop());
            monitoringService.stop(2000L);
        }

        if (outboundService != null) {
            outboundService.stop(2000L);
        }

        // Shutdown reply storage
        if (replyStorage != null) {
            replyStorage.shutdown();
        }

        getLogger().info("MineGate plugin disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public TelegramPollingService getPollingService() {
        return pollingService;
    }
}
