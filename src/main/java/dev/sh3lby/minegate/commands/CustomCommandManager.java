package dev.sh3lby.minegate.commands;

import dev.sh3lby.minegate.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CustomCommandManager {
    private static final String VANILLA_COMMAND_LISTENER_ERROR = "vanilla command listener";

    private final PluginConfig config;
    private final Logger logger;
    private final org.bukkit.plugin.Plugin plugin;

    public CustomCommandManager(PluginConfig config, Logger logger, org.bukkit.plugin.Plugin plugin) {
        this.config = config;
        this.logger = logger;
        this.plugin = plugin;
    }

    public List<String> executeCommand(String commandName, String[] args) {
        Map<String, PluginConfig.CustomCommandConfig> commands = config.getCustomCommands();
        if (commands == null || !commands.containsKey(commandName)) {
            return List.of("Команда '" + commandName + "' не найдена в конфигурации");
        }

        PluginConfig.CustomCommandConfig cmdConfig = commands.get(commandName);
        String commandTemplate = cmdConfig.getCommand();

        // Replace %player% placeholder if needed
        if (cmdConfig.isNeedPlayer()) {
            if (args.length == 0) {
                return List.of("Требуется указать имя игрока");
            }
            String playerName = args[0];
            commandTemplate = commandTemplate.replace("%player%", playerName);
        }

        // Create final copy for inner class
        final String finalCommandTemplate = commandTemplate;

        // Create interceptor
        CommandResultInterceptor interceptor = new CommandResultInterceptor(logger, cmdConfig.getMaxOutputLines());

        // Execute command in main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    dispatchAndCapture(finalCommandTemplate, interceptor);
                    
                    // Complete after a short delay to capture all output
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            interceptor.complete();
                        }
                    }.runTaskLater(plugin, 10L); // 0.5 seconds delay
                } catch (Exception e) {
                    logger.warning("Error executing command: " + e.getMessage());
                    interceptor.complete();
                }
            }
        }.runTask(plugin);

        // Wait for result (with timeout)
        try {
            Thread.sleep(6000); // Wait up to 6 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<String> result = interceptor.getResult();
        if (result == null || result.isEmpty()) {
            return List.of("Команда выполнена (нет вывода)");
        }

        return result;
    }

    @SuppressWarnings("removal")
    private void dispatchAndCapture(String command, CommandResultInterceptor interceptor) {
        CommandResultInterceptor.CapturingCommandSender sender = new CommandResultInterceptor.CapturingCommandSender();
        try {
            Bukkit.dispatchCommand(sender, command);
            interceptor.captureBuffer(sender.getBuffer());
        } catch (RuntimeException e) {
            if (!isVanillaCommandListenerError(e)) {
                throw e;
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            interceptor.captureMessage("Команда выполнена через консоль (вывод vanilla-команды смотрите в консоли сервера)");
        }
    }

    private boolean isVanillaCommandListenerError(RuntimeException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains(VANILLA_COMMAND_LISTENER_ERROR);
    }

    public String getHelpText() {
        Map<String, PluginConfig.CustomCommandConfig> commands = config.getCustomCommands();
        if (commands == null || commands.isEmpty()) {
            return "Нет доступных команд";
        }

        StringBuilder help = new StringBuilder("Доступные команды:\n");
        for (Map.Entry<String, PluginConfig.CustomCommandConfig> entry : commands.entrySet()) {
            String name = entry.getKey();
            PluginConfig.CustomCommandConfig cmdConfig = entry.getValue();
            help.append("/").append(name);
            if (cmdConfig.isNeedPlayer()) {
                help.append(" <player>");
            }
            help.append(" - ").append(cmdConfig.getDescription() != null ? cmdConfig.getDescription() : "").append("\n");
        }
        return help.toString().trim();
    }

    public boolean isCommandAvailable(String commandName) {
        Map<String, PluginConfig.CustomCommandConfig> commands = config.getCustomCommands();
        return commands != null && commands.containsKey(commandName);
    }
}
