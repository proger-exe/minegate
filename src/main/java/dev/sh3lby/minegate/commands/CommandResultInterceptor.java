package dev.sh3lby.minegate.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.BufferedCommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class CommandResultInterceptor {
    private final Logger logger;
    private final int maxLines;
    private final AtomicReference<List<String>> capturedOutput;
    private final CompletableFuture<List<String>> resultFuture;

    public CommandResultInterceptor(Logger logger, int maxLines) {
        this.logger = logger;
        this.maxLines = maxLines;
        this.capturedOutput = new AtomicReference<>(new ArrayList<>());
        this.resultFuture = new CompletableFuture<>();
    }

    public void captureMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        List<String> output = capturedOutput.get();
        if (output.size() < maxLines) {
            output.add(message);
        } else if (output.size() == maxLines) {
            output.add("... (output truncated)");
            resultFuture.complete(new ArrayList<>(output));
        }
    }

    public void captureBuffer(String buffer) {
        if (buffer == null || buffer.isBlank()) {
            return;
        }

        Arrays.stream(buffer.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .forEach(this::captureMessage);
    }

    public List<String> getResult() {
        try {
            // Wait up to 5 seconds for command output
            List<String> result = resultFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (java.util.concurrent.TimeoutException e) {
            // Timeout - return what we have
            List<String> output = capturedOutput.get();
            if (!output.isEmpty()) {
                return new ArrayList<>(output);
            }
        } catch (Exception e) {
            logger.warning("Failed to capture command output: " + e.getMessage());
        }
        
        // Return what we have so far
        List<String> output = capturedOutput.get();
        if (!output.isEmpty()) {
            if (!resultFuture.isDone()) {
                resultFuture.complete(new ArrayList<>(output));
            }
            return new ArrayList<>(output);
        }
        
        return List.of("Команда выполнена (нет вывода)");
    }

    public void complete() {
        List<String> output = capturedOutput.get();
        if (!resultFuture.isDone()) {
            resultFuture.complete(new ArrayList<>(output));
        }
    }

    @SuppressWarnings("removal")
    public static class CapturingCommandSender extends BufferedCommandSender {
        private final ConsoleCommandSender consoleSender;

        public CapturingCommandSender() {
            this.consoleSender = Bukkit.getConsoleSender();
        }

        @Override
        public String getName() {
            return consoleSender.getName();
        }

        @Override
        public boolean isOp() {
            return true;
        }

        @Override
        public void setOp(boolean value) {
            // Console-backed command execution should always keep full permissions.
        }

        @Override
        public boolean isPermissionSet(String name) {
            return true;
        }

        @Override
        public boolean isPermissionSet(Permission perm) {
            return true;
        }

        @Override
        public boolean hasPermission(String name) {
            return true;
        }

        @Override
        public boolean hasPermission(Permission perm) {
            return true;
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
            return consoleSender.addAttachment(plugin, name, value);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
            return consoleSender.addAttachment(plugin, name, value, ticks);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin) {
            return consoleSender.addAttachment(plugin);
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
            return consoleSender.addAttachment(plugin, ticks);
        }

        @Override
        public void removeAttachment(PermissionAttachment attachment) {
            consoleSender.removeAttachment(attachment);
        }

        @Override
        public void recalculatePermissions() {
            consoleSender.recalculatePermissions();
        }

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions() {
            return consoleSender.getEffectivePermissions();
        }

        @Override
        public Spigot spigot() {
            return consoleSender.spigot();
        }

        @Override
        public Component name() {
            return consoleSender.name();
        }

        @Override
        public Server getServer() {
            return consoleSender.getServer();
        }
    }
}
