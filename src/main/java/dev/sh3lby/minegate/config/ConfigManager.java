package dev.sh3lby.minegate.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private File configFile;
    private FileConfiguration config;
    private PluginConfig pluginConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void loadConfig() {
        // Save default config if not exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (Exception e) {
                logger.severe("Failed to save default config: " + e.getMessage());
                throw new RuntimeException("Config initialization failed", e);
            }
        }

        // Load config
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Parse into PluginConfig
        pluginConfig = new PluginConfig();

        // Telegram config
        PluginConfig.TelegramConfig telegramConfig = new PluginConfig.TelegramConfig();
        telegramConfig.setToken(config.getString("telegram.token", ""));
        telegramConfig.setChatId(config.getString("telegram.chat-id", ""));
        telegramConfig.setPollTimeoutSeconds(config.getInt("telegram.poll-timeout-seconds", 20));
        pluginConfig.setTelegram(telegramConfig);

        // Bridge config
        PluginConfig.BridgeConfig bridgeConfig = new PluginConfig.BridgeConfig();
        bridgeConfig.setEnabled(config.getBoolean("bridge.enabled", true));
        bridgeConfig.setReadOnly(config.getBoolean("bridge.read-only", false));
        bridgeConfig.setCooldownMs(config.getLong("bridge.cooldown-ms", 1000));
        bridgeConfig.setMaxMessageLength(config.getInt("bridge.max-message-length", 220));
        bridgeConfig.setEnableColorCodes(config.getBoolean("bridge.enable-color-codes", true));
        pluginConfig.setBridge(bridgeConfig);

        // Reply config
        PluginConfig.ReplyConfig replyConfig = new PluginConfig.ReplyConfig();
        replyConfig.setEnabled(config.getBoolean("reply.enabled", true));
        replyConfig.setMaxAgeSeconds(config.getLong("reply.max-age-seconds", 3600));
        pluginConfig.setReply(replyConfig);

        // Events config
        PluginConfig.EventsConfig eventsConfig = new PluginConfig.EventsConfig();
        eventsConfig.setJoin(config.getBoolean("events.join", false));
        eventsConfig.setQuit(config.getBoolean("events.quit", false));
        eventsConfig.setDeath(config.getBoolean("events.death", false));
        eventsConfig.setServer(config.getBoolean("events.server", false));
        pluginConfig.setEvents(eventsConfig);

        // Message formats config
        PluginConfig.MessageFormatsConfig formatsConfig = new PluginConfig.MessageFormatsConfig();
        formatsConfig.setMinecraftToTelegram(config.getString("formats.minecraft-to-telegram", "⛏ {player}: {message}"));
        formatsConfig.setTelegramToMinecraft(config.getString("formats.telegram-to-minecraft", "[TG] {name}: {message}"));
        formatsConfig.setSystemMessage(config.getString("formats.system-message", "⚙ {message}"));
        formatsConfig.setReplyIndicator(config.getString("formats.reply-indicator", "[в ответ {player}]"));
        formatsConfig.setReplyFormat(config.getString("formats.reply-format", "@{telegramName} {replyIndicator}: {message}"));
        pluginConfig.setFormats(formatsConfig);

        // Custom commands
        Map<String, PluginConfig.CustomCommandConfig> customCommands = new HashMap<>();
        if (config.contains("custom-commands") && config.isConfigurationSection("custom-commands")) {
            for (String key : config.getConfigurationSection("custom-commands").getKeys(false)) {
                String path = "custom-commands." + key;
                PluginConfig.CustomCommandConfig cmdConfig = new PluginConfig.CustomCommandConfig();
                cmdConfig.setNeedPlayer(config.getBoolean(path + ".need-player", false));
                cmdConfig.setCommand(config.getString(path + ".command", ""));
                cmdConfig.setDescription(config.getString(path + ".description", ""));
                cmdConfig.setMaxOutputLines(config.getInt(path + ".max-output-lines", 15));
                customCommands.put(key, cmdConfig);
            }
        }
        pluginConfig.setCustomCommands(customCommands);

        // Validate config
        validateConfig();
    }

    private void validateConfig() {
        if (pluginConfig.getTelegram().getToken().isEmpty()) {
            logger.warning("Telegram token is not set! Plugin may not work correctly.");
        }
        if (pluginConfig.getTelegram().getChatId().isEmpty()) {
            logger.warning("Telegram chat-id is not set! Plugin may not work correctly.");
        }
    }

    public PluginConfig getConfig() {
        return pluginConfig;
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}
