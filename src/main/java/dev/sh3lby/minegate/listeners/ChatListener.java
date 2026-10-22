package dev.sh3lby.minegate.listeners;

import dev.sh3lby.minegate.monitoring.MonitoringEvent;
import dev.sh3lby.minegate.monitoring.MonitoringService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final MonitoringService monitoringService;

    public ChatListener(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    // Paper 1.19+ removed AsyncPlayerChatEvent from firing — use AsyncChatEvent.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncChatEvent event) {
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        monitoringService.enqueue(MonitoringEvent.playerChat(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                plain
        ));
    }
}
