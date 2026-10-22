package dev.sh3lby.minegate.listeners;

import dev.sh3lby.minegate.monitoring.MonitoringEvent;
import dev.sh3lby.minegate.monitoring.MonitoringService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
    private final MonitoringService monitoringService;

    public EventListener(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        monitoringService.enqueue(MonitoringEvent.playerJoin(event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        monitoringService.enqueue(MonitoringEvent.playerQuit(event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        monitoringService.enqueue(MonitoringEvent.playerDeath(event.getDeathMessage()));
    }
}
